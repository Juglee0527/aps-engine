package com.github.juglee0527.apsengine.learning;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class LearningResultCoach {

    private static final Map<String, String> KPI_MEANINGS = Map.of(
            "MAKESPAN", "계획 시작부터 마지막 작업 완료까지 걸린 전체 시간",
            "TOTAL_TARDINESS", "각 오더의 납기 초과시간을 모두 더한 값",
            "DELAYED_ORDERS", "마지막 공정이 납기를 넘긴 생산오더 수",
            "UTILIZATION", "계획기간 가용시간 중 가공·준비작업이 사용한 비율",
            "TASK_COUNT", "현재 계획에 배치된 Operation 작업 수"
    );

    private final LearningScenarioCatalog catalog;

    public LearningResultCoach(LearningScenarioCatalog catalog) {
        this.catalog = catalog;
    }

    public LearningResultCoachResponse get(String scenarioKey) {
        LearningScenarioDefinition scenario = catalog.get(scenarioKey);
        Map<String, String> meanings = new LinkedHashMap<>();
        meanings.put("MAKESPAN", KPI_MEANINGS.get("MAKESPAN"));
        meanings.put("TASK_COUNT", KPI_MEANINGS.get("TASK_COUNT"));
        if (usesTardiness(scenario.key())) {
            meanings.put(
                    "TOTAL_TARDINESS",
                    KPI_MEANINGS.get("TOTAL_TARDINESS")
            );
            meanings.put(
                    "DELAYED_ORDERS",
                    KPI_MEANINGS.get("DELAYED_ORDERS")
            );
        }
        if (usesUtilization(scenario.key())) {
            meanings.put("UTILIZATION", KPI_MEANINGS.get("UTILIZATION"));
        }
        return new LearningResultCoachResponse(
                scenario.key(),
                scenario.objective(),
                scenario.observationPoints().stream()
                        .map(point -> point + "을(를) 결과에서 찾았나요?")
                        .toList(),
                meanings,
                scenario.resultExplanation(),
                scenario.nextExperiment()
        );
    }

    private boolean usesTardiness(String key) {
        return !key.equals("PRECEDENCE")
                && !key.equals("ALTERNATIVE_MACHINE");
    }

    private boolean usesUtilization(String key) {
        return key.equals("FINITE_CAPACITY")
                || key.equals("RULE_COMPARISON")
                || key.equals("BOTTLENECK")
                || key.equals("MEDIUM_FACTORY")
                || key.equals("PERFORMANCE");
    }
}
