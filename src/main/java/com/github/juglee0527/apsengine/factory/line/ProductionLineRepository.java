package com.github.juglee0527.apsengine.factory.line;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionLineRepository
        extends JpaRepository<ProductionLine, Long> {

    boolean existsByFactory_IdAndCode(Long factoryId, String code);
}

