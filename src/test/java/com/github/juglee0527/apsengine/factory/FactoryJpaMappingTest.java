package com.github.juglee0527.apsengine.factory;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("local")
@SpringBootTest
@Transactional
@Rollback
@EnabledIfEnvironmentVariable(
        named = "APS_POSTGRES_INTEGRATION_TEST",
        matches = "true"
)
class FactoryJpaMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndLoadsFactory() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        entityManager.persist(factory);
        entityManager.flush();

        Long factoryId = factory.id();
        entityManager.clear();

        Factory foundFactory = entityManager.find(Factory.class, factoryId);

        assertThat(foundFactory).isNotNull();
        assertThat(foundFactory.id()).isEqualTo(factoryId);
        assertThat(foundFactory.code()).isEqualTo("FACTORY-01");
        assertThat(foundFactory.name()).isEqualTo("서울 공장");
        assertThat(foundFactory.isActive()).isTrue();
    }
}

