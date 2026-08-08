package com.github.juglee0527.apsengine.learning;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningScenarioInstanceRepository
        extends JpaRepository<LearningScenarioInstance, Long> {

    Optional<LearningScenarioInstance> findByRequestKey(UUID requestKey);
}
