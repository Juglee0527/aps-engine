package com.github.juglee0527.apsengine.common.domain;

import java.util.Locale;

public final class BusinessCodeNormalizer {

    private static final String CODE_PATTERN = "[A-Z0-9][A-Z0-9_-]*";

    private BusinessCodeNormalizer() {
    }

    public static String normalize(
            String code,
            String fieldName,
            int maxLength
    ) {
        if (code == null) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        if (normalizedCode.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
        if (normalizedCode.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + "는 " + maxLength + "자를 초과할 수 없습니다."
            );
        }
        if (!normalizedCode.matches(CODE_PATTERN)) {
            throw new IllegalArgumentException(
                    fieldName + "는 영문, 숫자, 하이픈, 밑줄만 사용할 수 있습니다."
            );
        }
        return normalizedCode;
    }
}

