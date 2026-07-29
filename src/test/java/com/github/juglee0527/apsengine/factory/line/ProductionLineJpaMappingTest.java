package com.github.juglee0527.apsengine.factory.line;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.juglee0527.apsengine.factory.Factory;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
class ProductionLineJpaMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProductionLineRepository productionLineRepository;

    @Test
    void persistsProductionLineInFactory() {
        Factory factory = Factory.create("FACTORY-PL-09", "매핑 테스트 공장");
        entityManager.persist(factory);

        ProductionLine productionLine = ProductionLine.create(
                factory,
                "LINE-01",
                "조립 라인"
        );
        entityManager.persist(productionLine);
        entityManager.flush();

        Long productionLineId = productionLine.id();
        entityManager.clear();

        ProductionLine foundLine =
                entityManager.find(ProductionLine.class, productionLineId);

        assertThat(foundLine).isNotNull();
        assertThat(foundLine.factory().id()).isEqualTo(factory.id());
        assertThat(foundLine.code()).isEqualTo("LINE-01");
        assertThat(foundLine.name()).isEqualTo("조립 라인");
        assertThat(foundLine.isActive()).isTrue();
    }

    @Test
    void allowsSameLineCodeInDifferentFactories() {
        Factory firstFactory =
                Factory.create("FACTORY-PL-A", "첫 번째 공장");
        Factory secondFactory =
                Factory.create("FACTORY-PL-B", "두 번째 공장");
        entityManager.persist(firstFactory);
        entityManager.persist(secondFactory);

        entityManager.persist(ProductionLine.create(
                firstFactory,
                "LINE-01",
                "첫 번째 라인"
        ));
        entityManager.persist(ProductionLine.create(
                secondFactory,
                "LINE-01",
                "두 번째 라인"
        ));

        entityManager.flush();
    }

    @Test
    void loadsFactoryForPageResponseAfterPersistenceContextCloses() {
        Factory factory =
                Factory.create("FACTORY-PL-PAGE", "목록 조회 테스트 공장");
        entityManager.persist(factory);
        ProductionLine productionLine = ProductionLine.create(
                factory,
                "LINE-PAGE",
                "목록 조회 테스트 라인"
        );
        entityManager.persist(productionLine);
        entityManager.flush();
        entityManager.clear();

        Page<ProductionLine> page =
                productionLineRepository.findAllByFactory_Id(
                        factory.id(),
                        PageRequest.of(0, 20)
                );
        entityManager.clear();

        ProductionLineResponse response =
                ProductionLineResponse.from(page.getContent().getFirst());

        assertThat(response.factoryId()).isEqualTo(factory.id());
    }
}
