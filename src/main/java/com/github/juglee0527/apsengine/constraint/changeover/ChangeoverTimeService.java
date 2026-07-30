package com.github.juglee0527.apsengine.constraint.changeover;

import java.util.List;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeoverTimeService {

    private final MachineRepository machineRepository;
    private final ProductRepository productRepository;
    private final ChangeoverTimeRepository changeoverTimeRepository;

    public ChangeoverTimeService(
            MachineRepository machineRepository,
            ProductRepository productRepository,
            ChangeoverTimeRepository changeoverTimeRepository
    ) {
        this.machineRepository = machineRepository;
        this.productRepository = productRepository;
        this.changeoverTimeRepository = changeoverTimeRepository;
    }

    @Transactional
    public ChangeoverTime create(
            long machineId,
            long fromProductId,
            long toProductId,
            int changeoverMinutes
    ) {
        if (fromProductId == toProductId) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "동일 품목 사이에는 Changeover Time을 등록할 수 없습니다."
            );
        }
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.MACHINE_NOT_FOUND));
        Product fromProduct = getProduct(fromProductId);
        Product toProduct = getProduct(toProductId);
        ChangeoverTime changeoverTime = ChangeoverTime.create(
                machine,
                fromProduct,
                toProduct,
                changeoverMinutes
        );

        if (changeoverTimeRepository
                .existsByMachine_IdAndFromProduct_IdAndToProduct_Id(
                        machineId,
                        fromProductId,
                        toProductId
                )) {
            throw duplicatedException();
        }

        try {
            return changeoverTimeRepository.saveAndFlush(changeoverTime);
        } catch (DataIntegrityViolationException exception) {
            throw new ApplicationException(
                    ErrorCode.CHANGEOVER_TIME_DUPLICATED,
                    ErrorCode.CHANGEOVER_TIME_DUPLICATED.defaultMessage(),
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public ChangeoverTime getById(long changeoverTimeId) {
        return changeoverTimeRepository
                .findActiveDetailById(changeoverTimeId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.CHANGEOVER_TIME_NOT_FOUND
                ));
    }

    @Transactional(readOnly = true)
    public List<ChangeoverTime> getAllByMachine(long machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new ApplicationException(ErrorCode.MACHINE_NOT_FOUND);
        }
        return changeoverTimeRepository
                .findAllByMachine_IdAndActiveTrueOrderByFromProduct_IdAscToProduct_IdAsc(
                        machineId
                );
    }

    @Transactional(readOnly = true)
    public int resolveMinutes(
            long machineId,
            long fromProductId,
            long toProductId
    ) {
        if (fromProductId == toProductId) {
            return 0;
        }
        return changeoverTimeRepository
                .findByMachine_IdAndFromProduct_IdAndToProduct_IdAndActiveTrue(
                        machineId,
                        fromProductId,
                        toProductId
                )
                .map(ChangeoverTime::changeoverMinutes)
                .orElse(0);
    }

    private Product getProduct(long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private ApplicationException duplicatedException() {
        return new ApplicationException(
                ErrorCode.CHANGEOVER_TIME_DUPLICATED
        );
    }
}
