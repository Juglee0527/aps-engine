package com.github.juglee0527.apsengine.product;

public record ProductResponse(
        Long id,
        String code,
        String name,
        ProductUnit unit,
        boolean active
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.id(),
                product.code(),
                product.name(),
                product.unit(),
                product.isActive()
        );
    }
}
