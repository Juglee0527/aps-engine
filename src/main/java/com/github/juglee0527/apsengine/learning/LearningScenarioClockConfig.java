package com.github.juglee0527.apsengine.learning;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class LearningScenarioClockConfig {

    @Bean
    Clock learningScenarioClock() {
        return Clock.systemDefaultZone();
    }
}
