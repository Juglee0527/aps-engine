package com.github.juglee0527.apsengine.machine;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MachineCreateRequest(
        @NotBlank(message = "설비 코드는 필수입니다.")
        @Size(max = 50, message = "설비 코드는 50자를 초과할 수 없습니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "설비 코드는 영문, 숫자, 하이픈, 밑줄만 사용할 수 있습니다."
        )
        String code,

        @NotBlank(message = "설비 이름은 필수입니다.")
        @Size(max = 100, message = "설비 이름은 100자를 초과할 수 없습니다.")
        String name,

        @NotNull(message = "설비 상태는 필수입니다.")
        MachineStatus status
) {
}
