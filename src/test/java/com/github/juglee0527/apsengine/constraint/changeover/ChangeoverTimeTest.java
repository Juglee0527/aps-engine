package com.github.juglee0527.apsengine.constraint.changeover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductUnit;

import org.junit.jupiter.api.Test;

class ChangeoverTimeTest {

    @Test
    void createsDirectionalChangeoverTime() {
        Machine machine = machine();
        Product fromProduct = product("PRODUCT-A");
        Product toProduct = product("PRODUCT-B");

        ChangeoverTime changeoverTime = ChangeoverTime.create(
                machine,
                fromProduct,
                toProduct,
                30
        );

        assertThat(changeoverTime.machine()).isSameAs(machine);
        assertThat(changeoverTime.fromProduct()).isSameAs(fromProduct);
        assertThat(changeoverTime.toProduct()).isSameAs(toProduct);
        assertThat(changeoverTime.changeoverMinutes()).isEqualTo(30);
        assertThat(changeoverTime.isActive()).isTrue();
    }

    @Test
    void allowsDifferentDurationForReverseDirection() {
        Machine machine = machine();
        Product productA = product("PRODUCT-A");
        Product productB = product("PRODUCT-B");

        ChangeoverTime forward =
                ChangeoverTime.create(machine, productA, productB, 30);
        ChangeoverTime reverse =
                ChangeoverTime.create(machine, productB, productA, 10);

        assertThat(forward.changeoverMinutes()).isEqualTo(30);
        assertThat(reverse.changeoverMinutes()).isEqualTo(10);
        assertThat(forward.fromProduct()).isSameAs(reverse.toProduct());
    }

    @Test
    void rejectsSameProductTransition() {
        Product product = product("PRODUCT-A");

        assertThatThrownBy(() ->
                ChangeoverTime.create(machine(), product, product, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("동일 품목");
    }

    @Test
    void rejectsNegativeChangeoverMinutes() {
        assertThatThrownBy(() ->
                ChangeoverTime.create(
                        machine(),
                        product("PRODUCT-A"),
                        product("PRODUCT-B"),
                        -1
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0분 이상");
    }

    @Test
    void allowsZeroMinuteMappingForExplicitException() {
        ChangeoverTime changeoverTime = ChangeoverTime.create(
                machine(),
                product("PRODUCT-A"),
                product("PRODUCT-B"),
                0
        );

        assertThat(changeoverTime.changeoverMinutes()).isZero();
    }

    private Machine machine() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        return Machine.create(line, "MACHINE-01", "가공 설비");
    }

    private Product product(String code) {
        return Product.create(code, code, ProductUnit.PIECE);
    }
}
