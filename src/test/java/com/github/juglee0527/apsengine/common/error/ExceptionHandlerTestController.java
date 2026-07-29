package com.github.juglee0527.apsengine.common.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
class ExceptionHandlerTestController {

    @PostMapping("/requests")
    void create(@Valid @RequestBody TestRequest request) {
    }

    @GetMapping("/not-found")
    void notFound() {
        throw new ApplicationException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "테스트 리소스를 찾을 수 없습니다."
        );
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new IllegalStateException(
                "클라이언트에 노출되면 안 되는 내부 메시지"
        );
    }

    record TestRequest(
            @NotBlank(message = "이름은 필수입니다.")
            String name
    ) {
    }
}

