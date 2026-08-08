package com.github.juglee0527.apsengine.learning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.springframework.stereotype.Component;

@Component
public class LearningScenarioCatalog {

    private final Map<String, LearningScenarioDefinition> definitions;

    public LearningScenarioCatalog() {
        Map<String, LearningScenarioDefinition> values = new LinkedHashMap<>();
        register(values, new LearningScenarioDefinition(
                "FIRST_PLAN",
                "A",
                "첫 생산계획",
                "생산 자원과 공정을 준비하고 첫 유한능력 계획을 실행합니다.",
                3,
                3,
                8
        ));
        this.definitions = Collections.unmodifiableMap(values);
    }

    public List<LearningScenarioDefinition> findAll() {
        return List.copyOf(definitions.values());
    }

    public LearningScenarioDefinition get(String key) {
        String normalized = normalize(key);
        LearningScenarioDefinition definition = definitions.get(normalized);
        if (definition == null) {
            throw new ApplicationException(
                    ErrorCode.LEARNING_SCENARIO_NOT_FOUND
            );
        }
        return definition;
    }

    private void register(
            Map<String, LearningScenarioDefinition> values,
            LearningScenarioDefinition definition
    ) {
        values.put(definition.key(), definition);
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
    }
}
