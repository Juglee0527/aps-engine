package com.github.juglee0527.apsengine.factory;

public record FactoryResponse(
        Long id,
        String code,
        String name,
        boolean active
) {

    public static FactoryResponse from(Factory factory) {
        return new FactoryResponse(
                factory.id(),
                factory.code(),
                factory.name(),
                factory.isActive()
        );
    }
}

