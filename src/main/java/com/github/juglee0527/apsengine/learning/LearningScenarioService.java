package com.github.juglee0527.apsengine.learning;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningScenarioService {

    private final LearningScenarioCatalog catalog;
    private final LearningScenarioInstanceRepository instanceRepository;
    private final LearningScenarioEntityRepository entityRepository;
    private final LearningScenarioResetter resetter;
    private final Clock clock;

    public LearningScenarioService(
            LearningScenarioCatalog catalog,
            LearningScenarioInstanceRepository instanceRepository,
            LearningScenarioEntityRepository entityRepository,
            LearningScenarioResetter resetter,
            Clock clock
    ) {
        this.catalog = catalog;
        this.instanceRepository = instanceRepository;
        this.entityRepository = entityRepository;
        this.resetter = resetter;
        this.clock = clock;
    }

    public List<LearningScenarioDefinition> findScenarios() {
        return catalog.findAll();
    }

    @Transactional
    public LearningScenarioInstanceResponse create(
            String scenarioKey,
            UUID requestKey
    ) {
        LearningScenarioDefinition definition = catalog.get(scenarioKey);
        LearningScenarioInstance existing = instanceRepository
                .findByRequestKey(requestKey)
                .orElse(null);
        if (existing != null) {
            return matching(existing, definition.key());
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        LearningScenarioInstance instance = LearningScenarioInstance.create(
                requestKey,
                definition.key(),
                nextWorkingDayAtEight(clock),
                now
        );
        try {
            instance = instanceRepository.saveAndFlush(instance);
        } catch (DataIntegrityViolationException exception) {
            LearningScenarioInstance concurrent = instanceRepository
                    .findByRequestKey(requestKey)
                    .orElseThrow(() -> exception);
            return matching(concurrent, definition.key());
        }
        return response(instance);
    }

    @Transactional(readOnly = true)
    public LearningScenarioInstanceResponse find(long instanceId) {
        return response(required(instanceId));
    }

    @Transactional
    public LearningScenarioInstanceResponse reset(long instanceId) {
        LearningScenarioInstance instance = required(instanceId);
        if (instance.status() == LearningScenarioStatus.RESET) {
            return response(instance);
        }
        resetter.reset(instance);
        instance.reset();
        return response(instance);
    }

    @Transactional(readOnly = true)
    LearningScenarioPlanScope planScope(long instanceId) {
        LearningScenarioInstance instance = required(instanceId);
        if (instance.status() != LearningScenarioStatus.READY) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "초기화한 학습 시나리오는 계획할 수 없습니다."
            );
        }
        List<Long> orderIds = entityRepository
                .findAllByScenarioInstance_IdAndEntityTypeOrderByEntityIdAsc(
                        instanceId,
                        LearningScenarioEntityType.PRODUCTION_ORDER
                )
                .stream()
                .map(LearningScenarioEntity::entityId)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "학습 시나리오에 계획할 생산오더가 없습니다."
            );
        }
        return new LearningScenarioPlanScope(
                instance,
                instance.planningStart(),
                orderIds
        );
    }

    @Transactional
    void trackScheduleExecution(long instanceId, long executionId) {
        LearningScenarioInstance instance = required(instanceId);
        if (entityRepository
                .existsByScenarioInstance_IdAndEntityTypeAndEntityId(
                        instanceId,
                        LearningScenarioEntityType.SCHEDULE_EXECUTION,
                        executionId
                )) {
            return;
        }
        entityRepository.save(LearningScenarioEntity.create(
                instance,
                LearningScenarioEntityType.SCHEDULE_EXECUTION,
                executionId
        ));
    }

    private LearningScenarioInstanceResponse matching(
            LearningScenarioInstance instance,
            String scenarioKey
    ) {
        if (!instance.scenarioKey().equals(scenarioKey)) {
            throw new ApplicationException(
                    ErrorCode.LEARNING_SCENARIO_REQUEST_CONFLICT
            );
        }
        return response(instance);
    }

    private LearningScenarioInstance required(long instanceId) {
        if (instanceId < 1) {
            throw new ApplicationException(
                    ErrorCode.LEARNING_SCENARIO_INSTANCE_NOT_FOUND
            );
        }
        return instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.LEARNING_SCENARIO_INSTANCE_NOT_FOUND
                ));
    }

    private LearningScenarioInstanceResponse response(
            LearningScenarioInstance instance
    ) {
        return LearningScenarioInstanceResponse.from(
                instance,
                entityRepository.countByScenarioInstance_Id(instance.id())
        );
    }

    static OffsetDateTime nextWorkingDayAtEight(Clock clock) {
        ZoneId zone = clock.getZone();
        LocalDate date = LocalDate.now(clock).plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        ZonedDateTime start = ZonedDateTime.of(date, LocalTime.of(8, 0), zone);
        return start.toOffsetDateTime();
    }
}
