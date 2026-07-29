package com.github.juglee0527.apsengine.factory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FactoryCreateRequest(
        @NotBlank(message = "공장 코드는 필수입니다.")
        @Size(max = Factory.MAX_CODE_LENGTH, message = "공장 코드는 50자를 초과할 수 없습니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "공장 코드는 영문, 숫자, 하이픈, 밑줄만 사용할 수 있습니다."
        )
        String code,

        @NotBlank(message = "공장 이름은 필수입니다.")
        @Size(max = Factory.MAX_NAME_LENGTH, message = "공장 이름은 100자를 초과할 수 없습니다.")
        String name
) {
}

