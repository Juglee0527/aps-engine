package com.github.juglee0527.apsengine.learning;

import java.time.OffsetDateTime;
import java.util.List;

record LearningScenarioPlanScope(
        LearningScenarioInstance instance,
        OffsetDateTime planningStart,
        List<Long> productionOrderIds
) {
}
