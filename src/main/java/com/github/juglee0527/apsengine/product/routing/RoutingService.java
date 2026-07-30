package com.github.juglee0527.apsengine.product.routing;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.machine.MachineStatus;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoutingService {

    private final ProductRepository productRepository;
    private final MachineRepository machineRepository;
    private final RoutingRepository routingRepository;

    public RoutingService(
            ProductRepository productRepository,
            MachineRepository machineRepository,
            RoutingRepository routingRepository
    ) {
        this.productRepository = productRepository;
        this.machineRepository = machineRepository;
        this.routingRepository = routingRepository;
    }

    @Transactional
    public Routing create(
            long productId,
            String code,
            String name,
            List<OperationCreateRequest> operationRequests
    ) {
        Product product = getActiveProduct(productId);
        validateOperationDefinitions(operationRequests);
        Routing routing = Routing.create(product, code, name);

        if (routingRepository.existsByProduct_IdAndCode(
                productId,
                routing.code()
        )) {
            throw new ApplicationException(
                    ErrorCode.ROUTING_CODE_DUPLICATED
            );
        }

        for (OperationCreateRequest request : operationRequests) {
            Machine primaryMachine = getUsableMachine(request.machineId());
            Map<Machine, Integer> candidateDefinitions =
                    resolveCandidateDefinitions(request, primaryMachine);
            routing.addOperation(
                    request.sequence(),
                    request.code(),
                    request.name(),
                    request.processingTimeMinutes(),
                    primaryMachine,
                    candidateDefinitions
            );
        }

        try {
            return routingRepository.saveAndFlush(routing);
        } catch (DataIntegrityViolationException exception) {
            ErrorCode errorCode = ErrorCode.ROUTING_CODE_DUPLICATED;
            throw new ApplicationException(
                    errorCode,
                    errorCode.defaultMessage(),
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public Routing getById(long routingId) {
        if (routingId < 1) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "Routing ID는 1 이상이어야 합니다."
            );
        }
        return routingRepository.findDetailById(routingId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.ROUTING_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Routing> getAllByProduct(long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ApplicationException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return routingRepository.findAllDetailsByProductId(productId);
    }

    private Product getActiveProduct(long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isActive()) {
            throw new ApplicationException(ErrorCode.PRODUCT_INACTIVE);
        }
        return product;
    }

    private Machine getUsableMachine(long machineId) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.MACHINE_NOT_FOUND));
        if (machine.status() == MachineStatus.INACTIVE) {
            throw new ApplicationException(ErrorCode.MACHINE_INACTIVE);
        }
        return machine;
    }

    private Map<Machine, Integer> resolveCandidateDefinitions(
            OperationCreateRequest request,
            Machine primaryMachine
    ) {
        List<OperationMachineCandidateRequest> candidates =
                request.machineCandidates();
        if (candidates == null) {
            return Map.of(primaryMachine, 1);
        }
        if (candidates.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "Operation에는 후보 설비가 하나 이상 필요합니다."
            );
        }

        Set<Long> machineIds = new HashSet<>();
        Map<Machine, Integer> definitions = new LinkedHashMap<>();
        boolean primaryMachineIncluded = false;
        for (OperationMachineCandidateRequest candidate : candidates) {
            validateCandidate(candidate);
            if (!machineIds.add(candidate.machineId())) {
                throw new ApplicationException(
                        ErrorCode.INVALID_REQUEST,
                        "같은 설비를 Operation 후보로 중복 등록할 수 없습니다."
                );
            }

            Machine candidateMachine;
            if (candidate.machineId() == request.machineId()) {
                primaryMachineIncluded = true;
                if (candidate.priority() != 1) {
                    throw new ApplicationException(
                            ErrorCode.INVALID_REQUEST,
                            "주 설비의 후보 우선순위는 1이어야 합니다."
                    );
                }
                candidateMachine = primaryMachine;
            } else {
                candidateMachine = getUsableMachine(candidate.machineId());
            }
            definitions.put(candidateMachine, candidate.priority());
        }

        if (!primaryMachineIncluded) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "후보 설비에는 주 설비가 포함되어야 합니다."
            );
        }
        return definitions;
    }

    private void validateCandidate(
            OperationMachineCandidateRequest candidate
    ) {
        if (candidate == null
                || candidate.machineId() < 1
                || candidate.priority() < 1
                || candidate.priority()
                > OperationMachineCandidate.MAX_PRIORITY) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "후보 설비 ID와 우선순위를 확인해 주세요."
            );
        }
    }

    private void validateOperationDefinitions(
            List<OperationCreateRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "Routing에는 Operation이 하나 이상 필요합니다."
            );
        }

        Set<Integer> sequences = new HashSet<>();
        Set<String> normalizedCodes = new HashSet<>();
        for (OperationCreateRequest request : requests) {
            if (!sequences.add(request.sequence())) {
                throw new ApplicationException(
                        ErrorCode.INVALID_REQUEST,
                        "Routing 안에서 Operation 순서는 중복될 수 없습니다."
                );
            }
            String normalizedCode =
                    request.code().trim().toUpperCase(Locale.ROOT);
            if (!normalizedCodes.add(normalizedCode)) {
                throw new ApplicationException(
                        ErrorCode.INVALID_REQUEST,
                        "Routing 안에서 Operation 코드는 중복될 수 없습니다."
                );
            }
        }
    }
}
