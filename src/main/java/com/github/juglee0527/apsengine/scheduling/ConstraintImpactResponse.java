package com.github.juglee0527.apsengine.scheduling;

public record ConstraintImpactResponse(
        String scenarioKey,
        DispatchingRuleComparisonResult withoutConstraint,
        DispatchingRuleComparisonResult withConstraint,
        String explanation
) {
}
