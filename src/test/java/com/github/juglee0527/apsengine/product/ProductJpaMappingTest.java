package com.github.juglee0527.apsengine.product;

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
class ProductJpaMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndLoadsProduct() {
        Product product =
                Product.create("PRODUCT-MAP-01", "매핑 품목", ProductUnit.PIECE);
        entityManager.persist(product);
        entityManager.flush();
        Long productId = product.id();
        entityManager.clear();

        Product found = entityManager.find(Product.class, productId);

        assertThat(found.code()).isEqualTo("PRODUCT-MAP-01");
        assertThat(found.name()).isEqualTo("매핑 품목");
        assertThat(found.unit()).isEqualTo(ProductUnit.PIECE);
        assertThat(found.isActive()).isTrue();
    }
}
