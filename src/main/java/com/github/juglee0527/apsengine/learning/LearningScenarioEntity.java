package com.github.juglee0527.apsengine.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_scenario_entity")
public class LearningScenarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_instance_id", nullable = false)
    private LearningScenarioInstance scenarioInstance;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private LearningScenarioEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    protected LearningScenarioEntity() {
    }

    private LearningScenarioEntity(
            LearningScenarioInstance scenarioInstance,
            LearningScenarioEntityType entityType,
            long entityId
    ) {
        this.scenarioInstance = scenarioInstance;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public static LearningScenarioEntity create(
            LearningScenarioInstance scenarioInstance,
            LearningScenarioEntityType entityType,
            long entityId
    ) {
        if (scenarioInstance == null || entityType == null || entityId < 1) {
            throw new IllegalArgumentException("추적할 학습 시나리오 엔티티가 올바르지 않습니다.");
        }
        return new LearningScenarioEntity(scenarioInstance, entityType, entityId);
    }

    public Long id() { return id; }
    public LearningScenarioInstance scenarioInstance() { return scenarioInstance; }
    public LearningScenarioEntityType entityType() { return entityType; }
    public Long entityId() { return entityId; }
}
