package com.github.juglee0527.apsengine.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.juglee0527.apsengine.support.PostgreSqlContainerIntegrationTest;

import io.micrometer.core.instrument.MeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class SchedulingObservabilityPostgreSqlIntegrationTest
        extends PostgreSqlContainerIntegrationTest {

    @Autowired
    private ScheduleExecutionRepository executionRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void exposesHibernateQueryCountWhenStatisticsAreEnabled() {
        executionRepository.count();

        assertThat(meterRegistry.get("hibernate.query.executions")
                .functionCounter()
                .count()).isGreaterThanOrEqualTo(1);
    }
}
