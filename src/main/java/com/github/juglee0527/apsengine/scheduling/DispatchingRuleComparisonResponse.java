package com.github.juglee0527.apsengine.scheduling;

import java.util.List;

public record DispatchingRuleComparisonResponse(
        DispatchingRule recommendedRule,
        String recommendationReason,
        List<DispatchingRuleComparisonResult> results
) {
    public DispatchingRuleComparisonResponse {
        results = List.copyOf(results);
    }
}
