package com.github.juglee0527.apsengine.learning;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_scenario_instance")
public class LearningScenarioInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_key", nullable = false, unique = true)
    private UUID requestKey;

    @Column(name = "scenario_key", nullable = false, length = 50)
    private String scenarioKey;

    @Column(nullable = false, unique = true, length = 40)
    private String namespace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearningScenarioStatus status;

    @Column(name = "planning_start", nullable = false)
    private OffsetDateTime planningStart;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected LearningScenarioInstance() {
    }

    private LearningScenarioInstance(
            UUID requestKey,
            String scenarioKey,
            String namespace,
            OffsetDateTime planningStart,
            OffsetDateTime createdAt
    ) {
        this.requestKey = requestKey;
        this.scenarioKey = scenarioKey;
        this.namespace = namespace;
        this.status = LearningScenarioStatus.READY;
        this.planningStart = planningStart;
        this.createdAt = createdAt;
    }

    public static LearningScenarioInstance create(
            UUID requestKey,
            String scenarioKey,
            OffsetDateTime planningStart,
            OffsetDateTime createdAt
    ) {
        if (requestKey == null || scenarioKey == null || scenarioKey.isBlank()
                || planningStart == null || createdAt == null) {
            throw new IllegalArgumentException("학습 시나리오 인스턴스 값은 필수입니다.");
        }
        String namespace = "LEARN-" + requestKey.toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
        return new LearningScenarioInstance(
                requestKey,
                scenarioKey,
                namespace,
                planningStart,
                createdAt
        );
    }

    public void reset() {
        if (status == LearningScenarioStatus.RESET) {
            return;
        }
        status = LearningScenarioStatus.RESET;
    }

    public Long id() { return id; }
    public UUID requestKey() { return requestKey; }
    public String scenarioKey() { return scenarioKey; }
    public String namespace() { return namespace; }
    public LearningScenarioStatus status() { return status; }
    public OffsetDateTime planningStart() { return planningStart; }
    public OffsetDateTime createdAt() { return createdAt; }
}
