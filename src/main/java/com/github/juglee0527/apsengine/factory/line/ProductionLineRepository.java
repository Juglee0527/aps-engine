package com.github.juglee0527.apsengine.factory.line;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionLineRepository
        extends JpaRepository<ProductionLine, Long> {

    boolean existsByFactory_IdAndCode(Long factoryId, String code);

    @EntityGraph(attributePaths = "factory")
    Page<ProductionLine> findAllByFactory_Id(
            Long factoryId,
            Pageable pageable
    );
}
