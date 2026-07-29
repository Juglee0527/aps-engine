package com.github.juglee0527.apsengine.machine;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineRepository extends JpaRepository<Machine, Long> {

    boolean existsByProductionLine_IdAndCode(
            Long productionLineId,
            String code
    );

    @EntityGraph(attributePaths = "productionLine")
    Page<Machine> findAllByProductionLine_Id(
            Long productionLineId,
            Pageable pageable
    );
}
