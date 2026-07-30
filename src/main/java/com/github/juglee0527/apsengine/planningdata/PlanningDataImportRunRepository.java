package com.github.juglee0527.apsengine.planningdata;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanningDataImportRunRepository
        extends JpaRepository<PlanningDataImportRun, Long> {

    @EntityGraph(attributePaths = {"rows", "rows.errors"})
    Optional<PlanningDataImportRun> findByRequestKey(UUID requestKey);

    @Override
    @EntityGraph(attributePaths = {"rows", "rows.errors"})
    Optional<PlanningDataImportRun> findById(Long id);

    List<PlanningDataImportRun> findAllByStatus(
            PlanningDataImportStatus status
    );
}
