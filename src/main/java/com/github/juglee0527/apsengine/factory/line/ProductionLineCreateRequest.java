package com.github.juglee0527.apsengine.factory.line;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductionLineCreateRequest(
        @NotBlank(message = "생산라인 코드는 필수입니다.")
        @Size(max = 50, message = "생산라인 코드는 50자를 초과할 수 없습니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "생산라인 코드는 영문, 숫자, 하이픈, 밑줄만 사용할 수 있습니다."
        )
        String code,

        @NotBlank(message = "생산라인 이름은 필수입니다.")
        @Size(max = 100, message = "생산라인 이름은 100자를 초과할 수 없습니다.")
        String name
) {
}

