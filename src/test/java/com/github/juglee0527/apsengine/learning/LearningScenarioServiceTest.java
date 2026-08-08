package com.github.juglee0527.apsengine.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
}
