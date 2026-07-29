package com.github.juglee0527.apsengine.product.routing;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoutingCreateRequest(
        @NotBlank(message = "Routing 코드는 필수입니다.")
        @Size(max = 50, message = "Routing 코드는 50자를 초과할 수 없습니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "Routing 코드는 영문, 숫자, 하이픈, 밑줄만 사용할 수 있습니다."
        )
        String code,

        @NotBlank(message = "Routing 이름은 필수입니다.")
        @Size(max = 100, message = "Routing 이름은 100자를 초과할 수 없습니다.")
        String name,

        @NotEmpty(message = "Routing에는 Operation이 하나 이상 필요합니다.")
        List<@Valid OperationCreateRequest> operations
) {

    public RoutingCreateRequest {
        operations = operations == null ? null : List.copyOf(operations);
    }
}
