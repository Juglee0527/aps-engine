package com.github.juglee0527.apsengine.learning;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningScenarioEntityRepository
        extends JpaRepository<LearningScenarioEntity, Long> {

    List<LearningScenarioEntity> findAllByScenarioInstance_IdOrderByIdDesc(
            Long scenarioInstanceId
    );

    List<LearningScenarioEntity>
            findAllByScenarioInstance_IdAndEntityTypeOrderByEntityIdAsc(
                    Long scenarioInstanceId,
                    LearningScenarioEntityType entityType
            );

    long countByScenarioInstance_Id(Long scenarioInstanceId);

    boolean existsByScenarioInstance_IdAndEntityTypeAndEntityId(
            Long scenarioInstanceId,
            LearningScenarioEntityType entityType,
            Long entityId
    );

    void deleteAllByScenarioInstance_Id(Long scenarioInstanceId);
}
