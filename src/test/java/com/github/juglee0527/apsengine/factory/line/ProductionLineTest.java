package com.github.juglee0527.apsengine.factory.line;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.juglee0527.apsengine.factory.Factory;

import org.junit.jupiter.api.Test;

class ProductionLineTest {

    @Test
    void createsActiveProductionLineWithNormalizedValues() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");

        ProductionLine productionLine = ProductionLine.create(
                factory,
                "  line-01  ",
                "  조립 라인  "
        );

        assertThat(productionLine.id()).isNull();
        assertThat(productionLine.factory()).isSameAs(factory);
        assertThat(productionLine.code()).isEqualTo("LINE-01");
        assertThat(productionLine.name()).isEqualTo("조립 라인");
        assertThat(productionLine.isActive()).isTrue();
    }

    @Test
    void rejectsInactiveFactory() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        factory.deactivate();

        assertThatThrownBy(() -> ProductionLine.create(
                factory,
                "LINE-01",
                "조립 라인"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("비활성 공장에는 생산라인을 등록할 수 없습니다.");
    }

    @Test
    void rejectsInvalidCodeAndName() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");

        assertThatThrownBy(() ->
                ProductionLine.create(factory, "line 01", "조립 라인"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                ProductionLine.create(factory, "LINE-01", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

