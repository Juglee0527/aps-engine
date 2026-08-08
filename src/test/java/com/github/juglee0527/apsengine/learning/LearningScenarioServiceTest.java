package com.github.juglee0527.apsengine.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LearningScenarioServiceTest {

    @Mock
    private LearningScenarioInstanceRepository instanceRepository;

    @Mock
    private LearningScenarioEntityRepository entityRepository;

    @Mock
    private LearningScenarioResetter resetter;

    @Mock
    private LearningScenarioProvisioner provisioner;

    private LearningScenarioService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-07T01:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new LearningScenarioService(
                new LearningScenarioCatalog(),
                instanceRepository,
                entityRepository,
                resetter,
                provisioner,
                clock
        );
    }

    @Test
    void createsInstanceForNextWorkingDayAndReusesRequest() {
        UUID requestKey = UUID.randomUUID();
        when(instanceRepository.findByRequestKey(requestKey))
                .thenReturn(Optional.empty());
        when(instanceRepository.saveAndFlush(
                org.mockito.ArgumentMatchers.any()
        )).thenAnswer(invocation -> {
            LearningScenarioInstance instance = invocation.getArgument(0);
            ReflectionTestUtils.setField(instance, "id", 7L);
            return instance;
        });
        when(entityRepository.countByScenarioInstance_Id(7L)).thenReturn(0L);

        LearningScenarioInstanceResponse created = service.create(
                "FIRST_PLAN",
                requestKey
        );

        assertThat(created.id()).isEqualTo(7L);
        assertThat(created.planningStart().getDayOfWeek().getValue())
                .isEqualTo(1);
        assertThat(created.planningStart().getHour()).isEqualTo(8);
        verify(provisioner).provision(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void rejectsSameRequestKeyForDifferentScenario() {
        UUID requestKey = UUID.randomUUID();
        LearningScenarioInstance existing = LearningScenarioInstance.create(
                requestKey,
                "ANOTHER_SCENARIO",
                java.time.OffsetDateTime.now(),
                java.time.OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(existing, "id", 3L);
        when(instanceRepository.findByRequestKey(requestKey))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create("FIRST_PLAN", requestKey))
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(
                                ErrorCode.LEARNING_SCENARIO_REQUEST_CONFLICT
                        )
                );
    }

    @Test
    void resetsOnlyTrackedInstanceEntities() {
        LearningScenarioInstance instance = LearningScenarioInstance.create(
                UUID.randomUUID(),
                "FIRST_PLAN",
                java.time.OffsetDateTime.now(),
                java.time.OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(instance, "id", 9L);
        when(instanceRepository.findById(9L))
                .thenReturn(Optional.of(instance));
        when(entityRepository.countByScenarioInstance_Id(9L)).thenReturn(0L);

        LearningScenarioInstanceResponse response = service.reset(9L);

        verify(resetter).reset(instance);
        assertThat(response.status()).isEqualTo(LearningScenarioStatus.RESET);
    }

    @Test
    void buildsPlanScopeOnlyFromTrackedProductionOrders() {
        LearningScenarioInstance instance = LearningScenarioInstance.create(
                UUID.randomUUID(),
                "FIRST_PLAN",
                java.time.OffsetDateTime.now(),
                java.time.OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(instance, "id", 12L);
        when(instanceRepository.findById(12L))
                .thenReturn(Optional.of(instance));
        when(entityRepository
                .findAllByScenarioInstance_IdAndEntityTypeOrderByEntityIdAsc(
                        12L,
                        LearningScenarioEntityType.PRODUCTION_ORDER
                )).thenReturn(List.of(
                        LearningScenarioEntity.create(
                                instance,
                                LearningScenarioEntityType.PRODUCTION_ORDER,
                                21L
                        ),
                        LearningScenarioEntity.create(
                                instance,
                                LearningScenarioEntityType.PRODUCTION_ORDER,
                                21L
                        ),
                        LearningScenarioEntity.create(
                                instance,
                                LearningScenarioEntityType.PRODUCTION_ORDER,
                                22L
                        )
                ));

        LearningScenarioPlanScope scope = service.planScope(12L);

        assertThat(scope.productionOrderIds()).containsExactly(21L, 22L);
        assertThat(scope.planningStart())
                .isEqualTo(instance.planningStart());
    }

    @Test
    void rejectsPlanScopeWithoutTrackedOrders() {
        LearningScenarioInstance instance = LearningScenarioInstance.create(
                UUID.randomUUID(),
                "FIRST_PLAN",
                java.time.OffsetDateTime.now(),
                java.time.OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(instance, "id", 13L);
        when(instanceRepository.findById(13L))
                .thenReturn(Optional.of(instance));
        when(entityRepository
                .findAllByScenarioInstance_IdAndEntityTypeOrderByEntityIdAsc(
                        13L,
                        LearningScenarioEntityType.PRODUCTION_ORDER
                )).thenReturn(List.of());

        assertThatThrownBy(() -> service.planScope(13L))
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.INVALID_REQUEST)
                );
    }
}
