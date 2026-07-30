package com.github.juglee0527.apsengine.scheduling;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class ScheduleExecutionTransactionService {

    private final ScheduleExecutionRepository executionRepository;
    private final ScheduleRunRepository scheduleRunRepository;

    ScheduleExecutionTransactionService(
            ScheduleExecutionRepository executionRepository,
            ScheduleRunRepository scheduleRunRepository
    ) {
        this.executionRepository = executionRepository;
        this.scheduleRunRepository = scheduleRunRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScheduleExecutionQueueResult queue(
            UUID executionKey,
            OffsetDateTime planningStart,
            DispatchingRule dispatchingRule
    ) {
        validateNormalRequest(
                executionKey,
                planningStart,
                dispatchingRule
        );
        ScheduleExecution existing = executionRepository
                .findByExecutionKey(executionKey)
                .orElse(null);
        if (existing != null) {
            requireMatching(
                    existing,
                    planningStart,
                    dispatchingRule,
                    null,
                    null
            );
            return existingResult(existing);
        }
        ScheduleExecution queued = ScheduleExecution.queue(
                executionKey,
                planningStart,
                dispatchingRule,
                OffsetDateTime.now()
        );
        executionRepository.saveAndFlush(queued);
        return new ScheduleExecutionQueueResult(queued.id(), true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScheduleExecutionQueueResult queueReschedule(
            long sourceScheduleRunId,
            UUID executionKey,
            OffsetDateTime frozenAt,
            DispatchingRule requestedRule
    ) {
        if (sourceScheduleRunId < 1
                || executionKey == null
                || frozenAt == null) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
        ScheduleRun source = scheduleRunRepository
                .findById(sourceScheduleRunId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.SCHEDULE_RUN_NOT_FOUND
                ));
        if (frozenAt.isBefore(source.planningStart())) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "동결 기준시각은 원본 계획 시작시각보다 이전일 수 없습니다."
            );
        }
        DispatchingRule dispatchingRule = requestedRule == null
                ? source.dispatchingRule()
                : requestedRule;
        ScheduleExecution existing = executionRepository
                .findByExecutionKey(executionKey)
                .orElse(null);
        if (existing != null) {
            requireMatching(
                    existing,
                    source.planningStart(),
                    dispatchingRule,
                    sourceScheduleRunId,
                    frozenAt
            );
            return existingResult(existing);
        }
        ScheduleExecution queued =
                ScheduleExecution.queueReschedule(
                        executionKey,
                        source,
                        frozenAt,
                        dispatchingRule,
                        OffsetDateTime.now()
                );
        executionRepository.saveAndFlush(queued);
        return new ScheduleExecutionQueueResult(queued.id(), true);
    }

    @Transactional(readOnly = true)
    public ScheduleExecutionResponse findMatching(
            UUID executionKey,
            OffsetDateTime planningStart,
            DispatchingRule dispatchingRule
    ) {
        ScheduleExecution existing =
                requireByExecutionKey(executionKey);
        requireMatching(
                existing,
                planningStart,
                dispatchingRule,
                null,
                null
        );
        return ScheduleExecutionResponse.from(existing);
    }

    @Transactional(readOnly = true)
    public ScheduleExecutionResponse findMatchingReschedule(
            long sourceScheduleRunId,
            UUID executionKey,
            OffsetDateTime frozenAt,
            DispatchingRule requestedRule
    ) {
        ScheduleExecution existing =
                requireByExecutionKey(executionKey);
        DispatchingRule dispatchingRule = requestedRule == null
                ? existing.dispatchingRule()
                : requestedRule;
        requireMatching(
                existing,
                existing.planningStart(),
                dispatchingRule,
                sourceScheduleRunId,
                frozenAt
        );
        return ScheduleExecutionResponse.from(existing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScheduleExecutionSnapshot start(Long executionId) {
        ScheduleExecution execution = requireExecution(executionId);
        if (execution.status() != ScheduleExecutionStatus.QUEUED) {
            return null;
        }
        execution.start(OffsetDateTime.now());
        executionRepository.saveAndFlush(execution);
        return snapshot(execution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long executionId, Long scheduleRunId) {
        ScheduleExecution execution = requireExecution(executionId);
        ScheduleRun scheduleRun = scheduleRunRepository
                .findById(scheduleRunId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.SCHEDULE_RUN_NOT_FOUND
                ));
        execution.complete(scheduleRun, OffsetDateTime.now());
        executionRepository.saveAndFlush(execution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long executionId, String reason) {
        ScheduleExecution execution = requireExecution(executionId);
        if (execution.status() != ScheduleExecutionStatus.QUEUED
                && execution.status()
                != ScheduleExecutionStatus.RUNNING) {
            return;
        }
        execution.fail(reason, OffsetDateTime.now());
        executionRepository.saveAndFlush(execution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int reconcileRunning() {
        List<ScheduleExecution> running = executionRepository
                .findAllByStatusOrderByCreatedAtAscIdAsc(
                        ScheduleExecutionStatus.RUNNING
                );
        int recovered = 0;
        for (ScheduleExecution execution : running) {
            ScheduleRun result = scheduleRunRepository
                    .findByExecutionKey(execution.executionKey())
                    .orElse(null);
            if (result == null) {
                execution.fail(
                        "애플리케이션 재시작으로 스케줄 계산이 중단되었습니다.",
                        OffsetDateTime.now()
                );
            } else {
                execution.complete(result, OffsetDateTime.now());
            }
            recovered++;
        }
        executionRepository.saveAllAndFlush(running);
        return recovered;
    }

    @Transactional(readOnly = true)
    public List<Long> findQueuedIds() {
        return executionRepository
                .findAllByStatusOrderByCreatedAtAscIdAsc(
                        ScheduleExecutionStatus.QUEUED
                )
                .stream()
                .map(ScheduleExecution::id)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleExecutionResponse find(Long executionId) {
        return ScheduleExecutionResponse.from(
                requireExecution(executionId)
        );
    }

    @Transactional(readOnly = true)
    public List<ScheduleExecutionResponse> findRecent(int limit) {
        if (limit < 1 || limit > 100) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "limit은 1 이상 100 이하여야 합니다."
            );
        }
        return executionRepository
                .findAllByOrderByCreatedAtDescIdDesc(
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(ScheduleExecutionResponse::from)
                .toList();
    }

    private void validateNormalRequest(
            UUID executionKey,
            OffsetDateTime planningStart,
            DispatchingRule dispatchingRule
    ) {
        if (executionKey == null
                || planningStart == null
                || dispatchingRule == null) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
    }

    private ScheduleExecution requireExecution(Long executionId) {
        return executionRepository.findById(executionId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.SCHEDULE_EXECUTION_NOT_FOUND
                ));
    }

    private ScheduleExecution requireByExecutionKey(UUID executionKey) {
        return executionRepository.findByExecutionKey(executionKey)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.SCHEDULE_EXECUTION_NOT_FOUND
                ));
    }

    private void requireMatching(
            ScheduleExecution existing,
            OffsetDateTime planningStart,
            DispatchingRule dispatchingRule,
            Long sourceScheduleRunId,
            OffsetDateTime frozenAt
    ) {
        if (!existing.matches(
                planningStart,
                dispatchingRule,
                sourceScheduleRunId,
                frozenAt
        )) {
            throw new ApplicationException(
                    ErrorCode.SCHEDULE_EXECUTION_REQUEST_CONFLICT
            );
        }
    }

    private ScheduleExecutionQueueResult existingResult(
            ScheduleExecution existing
    ) {
        return new ScheduleExecutionQueueResult(existing.id(), false);
    }

    private ScheduleExecutionSnapshot snapshot(
            ScheduleExecution execution
    ) {
        return new ScheduleExecutionSnapshot(
                execution.id(),
                execution.executionKey(),
                execution.planningStart(),
                execution.dispatchingRule(),
                execution.sourceScheduleRunId(),
                execution.frozenAt()
        );
    }
}
