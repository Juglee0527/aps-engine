package com.github.juglee0527.apsengine.factory.line;

import java.net.URI;

import com.github.juglee0527.apsengine.common.web.PageRequestParameters;
import com.github.juglee0527.apsengine.common.web.PageResponse;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/factories/{factoryId}/production-lines")
public class ProductionLineController {

    private final ProductionLineService productionLineService;

    public ProductionLineController(
            ProductionLineService productionLineService
    ) {
        this.productionLineService = productionLineService;
    }

    @PostMapping
    public ResponseEntity<ProductionLineResponse> create(
            @PathVariable long factoryId,
            @Valid @RequestBody ProductionLineCreateRequest request
    ) {
        ProductionLine productionLine = productionLineService.create(
                factoryId,
                request.code(),
                request.name()
        );
        ProductionLineResponse response =
                ProductionLineResponse.from(productionLine);
        URI location = URI.create(
                "/api/v1/factories/%d/production-lines/%d".formatted(
                        factoryId,
                        response.id()
                )
        );

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public PageResponse<ProductionLineResponse> getPage(
            @PathVariable long factoryId,
            @Valid @ModelAttribute PageRequestParameters request
    ) {
        Page<ProductionLine> productionLinePage =
                productionLineService.getPageByFactory(
                        factoryId,
                        request.page(),
                        request.size()
                );
        return PageResponse.from(
                productionLinePage,
                ProductionLineResponse::from
        );
    }
}
