package com.github.juglee0527.apsengine.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BusinessCodeNormalizerTest {

    @Test
    void normalizesBusinessCode() {
        assertThat(BusinessCodeNormalizer.normalize(
                "  code-01  ",
                "코드",
                50
        )).isEqualTo("CODE-01");
    }

    @Test
    void rejectsInvalidBusinessCode() {
        assertThatThrownBy(() -> BusinessCodeNormalizer.normalize(
                "code 01",
                "코드",
                50
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("코드는 영문, 숫자, 하이픈, 밑줄만 사용할 수 있습니다.");
    }
}

