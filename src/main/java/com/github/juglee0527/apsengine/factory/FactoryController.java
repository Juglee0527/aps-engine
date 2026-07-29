package com.github.juglee0527.apsengine.factory;

import java.net.URI;

import com.github.juglee0527.apsengine.common.web.PageRequestParameters;
import com.github.juglee0527.apsengine.common.web.PageResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/factories")
public class FactoryController {

    private final FactoryService factoryService;

    public FactoryController(FactoryService factoryService) {
        this.factoryService = factoryService;
    }

    @PostMapping
    public ResponseEntity<FactoryResponse> create(
            @Valid @RequestBody FactoryCreateRequest request
    ) {
        Factory factory = factoryService.create(request.code(), request.name());
        FactoryResponse response = FactoryResponse.from(factory);
        URI location = URI.create("/api/v1/factories/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{factoryId}")
    public FactoryResponse getById(@PathVariable long factoryId) {
        Factory factory = factoryService.getById(factoryId);
        return FactoryResponse.from(factory);
    }

    @GetMapping
    public PageResponse<FactoryResponse> getPage(
            @Valid @ModelAttribute PageRequestParameters request
    ) {
        Page<Factory> factoryPage =
                factoryService.getPage(request.page(), request.size());
        return PageResponse.from(factoryPage, FactoryResponse::from);
    }
}
