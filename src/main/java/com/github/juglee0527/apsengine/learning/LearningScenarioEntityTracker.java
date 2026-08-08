package com.github.juglee0527.apsengine.learning;

import org.springframework.stereotype.Component;

@Component
public class LearningScenarioEntityTracker {

    private final LearningScenarioEntityRepository repository;

    public LearningScenarioEntityTracker(
            LearningScenarioEntityRepository repository
    ) {
        this.repository = repository;
    }

    public void track(
            LearningScenarioInstance instance,
            LearningScenarioEntityType type,
            long entityId
    ) {
        repository.save(LearningScenarioEntity.create(
                instance,
                type,
                entityId
        ));
    }
}
