package com.github.juglee0527.apsengine.learning;

import java.util.List;
import java.util.Map;

record LearningScenarioBlueprint(
        String key,
        String course,
        String title,
        String description,
        String objective,
        String predictionPrompt,
        List<String> observationPoints,
        String resultExplanation,
        String nextExperiment,
        List<MachineSpec> machines,
        List<ProductSpec> products,
        List<OrderSpec> orders
) {
    LearningScenarioDefinition definition() {
        return new LearningScenarioDefinition(
                key,
                course,
                title,
                description,
                machines.size(),
                products.size(),
                orders.size(),
                objective,
                predictionPrompt,
                observationPoints,
                resultExplanation,
                nextExperiment
        );
    }

    record MachineSpec(String code, String name) {
    }

    record ProductSpec(
            String code,
            String name,
            List<OperationSpec> operations
    ) {
    }

    record OperationSpec(
            int sequence,
            String code,
            String name,
            int processingMinutes,
            String machineCode,
            Map<String, Integer> machineCandidates
    ) {
        OperationSpec(
                int sequence,
                String code,
                String name,
                int processingMinutes,
                String machineCode
        ) {
            this(
                    sequence,
                    code,
                    name,
                    processingMinutes,
                    machineCode,
                    Map.of(machineCode, 1)
            );
        }

        OperationSpec {
            machineCandidates = Map.copyOf(machineCandidates);
        }
    }

    record OrderSpec(
            String productCode,
            String orderNumber,
            long quantity,
            long releaseOffsetMinutes,
            long dueOffsetMinutes,
            int priority
    ) {
    }
}
