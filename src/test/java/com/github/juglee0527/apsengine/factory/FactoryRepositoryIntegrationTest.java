package com.github.juglee0527.apsengine.factory;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.juglee0527.apsengine.support.PostgreSqlContainerIntegrationTest;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Rollback
class FactoryRepositoryIntegrationTest
        extends PostgreSqlContainerIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private FactoryRepository factoryRepository;

    @Test
    void persistsAndLoadsFactory() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        Factory savedFactory =
                factoryRepository.saveAndFlush(factory);

        Long factoryId = savedFactory.id();
        entityManager.clear();

        Factory foundFactory = factoryRepository.findById(factoryId)
                .orElseThrow();

        assertThat(foundFactory).isNotNull();
        assertThat(foundFactory.id()).isEqualTo(factoryId);
        assertThat(foundFactory.code()).isEqualTo("FACTORY-01");
        assertThat(foundFactory.name()).isEqualTo("서울 공장");
        assertThat(foundFactory.isActive()).isTrue();
    }
}
