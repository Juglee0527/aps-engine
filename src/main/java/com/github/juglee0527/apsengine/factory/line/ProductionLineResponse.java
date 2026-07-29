package com.github.juglee0527.apsengine.factory.line;

public record ProductionLineResponse(
        Long id,
        Long factoryId,
        String code,
        String name,
        boolean active
) {

    public static ProductionLineResponse from(ProductionLine productionLine) {
        return new ProductionLineResponse(
                productionLine.id(),
                productionLine.factory().id(),
                productionLine.code(),
                productionLine.name(),
                productionLine.isActive()
        );
    }
}

