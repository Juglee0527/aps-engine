package com.github.juglee0527.apsengine.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void createsActiveProductWithNormalizedValues() {
        Product product = Product.create(
                " product-01 ",
                "  완제품 A  ",
                ProductUnit.PIECE
        );

        assertThat(product.code()).isEqualTo("PRODUCT-01");
        assertThat(product.name()).isEqualTo("완제품 A");
        assertThat(product.unit()).isEqualTo(ProductUnit.PIECE);
        assertThat(product.isActive()).isTrue();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Product.create(
                "PRODUCT-01",
                " ",
                ProductUnit.PIECE
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("품목 이름은 필수입니다.");
    }

    @Test
    void rejectsMissingUnit() {
        assertThatThrownBy(() -> Product.create(
                "PRODUCT-01",
                "완제품 A",
                null
        )).isInstanceOf(NullPointerException.class);
    }
}
