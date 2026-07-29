package com.github.juglee0527.apsengine.factory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FactoryRepository extends JpaRepository<Factory, Long> {

    boolean existsByCode(String code);
}

