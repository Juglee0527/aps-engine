package com.github.juglee0527.apsengine.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FactoryTest {

    @Test
    void createsActiveFactoryWithNormalizedValues() {
        Factory factory = Factory.create("  factory-01  ", "  서울 공장  ");

        assertThat(factory.id()).isNull();
        assertThat(factory.code()).isEqualTo("FACTORY-01");
        assertThat(factory.name()).isEqualTo("서울 공장");
        assertThat(factory.isActive()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "factory 01", "factory@01"})
    void rejectsInvalidFactoryCode(String code) {
        assertThatThrownBy(() -> Factory.create(code, "서울 공장"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFactoryCodeLongerThanMaximumLength() {
        String tooLongCode = "A".repeat(Factory.MAX_CODE_LENGTH + 1);

        assertThatThrownBy(() -> Factory.create(tooLongCode, "서울 공장"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("공장 코드는 50자를 초과할 수 없습니다.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void rejectsBlankFactoryName(String name) {
        assertThatThrownBy(() -> Factory.create("FACTORY-01", name))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("공장 이름은 필수입니다.");
    }

    @Test
    void rejectsFactoryNameLongerThanMaximumLength() {
        String tooLongName = "가".repeat(Factory.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> Factory.create("FACTORY-01", tooLongName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("공장 이름은 100자를 초과할 수 없습니다.");
    }

    @Test
    void renamesFactoryWithNormalizedName() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");

        factory.rename("  부산 공장  ");

        assertThat(factory.name()).isEqualTo("부산 공장");
    }

    @Test
    void changesActiveStateExplicitly() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");

        factory.deactivate();
        assertThat(factory.isActive()).isFalse();

        factory.activate();
        assertThat(factory.isActive()).isTrue();
    }
}

