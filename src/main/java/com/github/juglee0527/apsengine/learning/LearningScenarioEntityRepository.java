package com.github.juglee0527.apsengine.learning;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningScenarioEntityRepository
        extends JpaRepository<LearningScenarioEntity, Long> {

    List<LearningScenarioEntity> findAllByScenarioInstance_IdOrderByIdDesc(
            Long scenarioInstanceId
    );

    long countByScenarioInstance_Id(Long scenarioInstanceId);

    void deleteAllByScenarioInstance_Id(Long scenarioInstanceId);
}
