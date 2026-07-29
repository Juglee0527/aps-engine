package com.github.juglee0527.apsengine.product;

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
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        Product product = productService.create(
                request.code(),
                request.name(),
                request.unit()
        );
        ProductResponse response = ProductResponse.from(product);
        URI location = URI.create("/api/v1/products/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{productId}")
    public ProductResponse getById(@PathVariable long productId) {
        return ProductResponse.from(productService.getById(productId));
    }

    @GetMapping
    public PageResponse<ProductResponse> getPage(
            @Valid @ModelAttribute PageRequestParameters request
    ) {
        Page<Product> productPage =
                productService.getPage(request.page(), request.size());
        return PageResponse.from(productPage, ProductResponse::from);
    }
}
