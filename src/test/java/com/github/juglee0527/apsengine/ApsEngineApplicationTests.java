package com.github.juglee0527.apsengine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

import com.github.juglee0527.apsengine.factory.FactoryRepository;
import com.github.juglee0527.apsengine.capacity.WorkingCalendarRepository;
import com.github.juglee0527.apsengine.constraint.changeover.ChangeoverTimeRepository;
import com.github.juglee0527.apsengine.constraint.maintenance.MachineMaintenanceRepository;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;
import com.github.juglee0527.apsengine.machine.MachineRepository;
import com.github.juglee0527.apsengine.learning.LearningScenarioEntityRepository;
import com.github.juglee0527.apsengine.learning.LearningScenarioInstanceRepository;
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;
import com.github.juglee0527.apsengine.planningdata.PlanningDataImportRunRepository;
import com.github.juglee0527.apsengine.scheduling.ScheduleRunRepository;
import com.github.juglee0527.apsengine.scheduling.ScheduleExecutionRepository;
import com.github.juglee0527.apsengine.scheduling.ScheduledOperationRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@AutoConfigureMockMvc
class ApsEngineApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FactoryRepository factoryRepository;

    @MockitoBean
    private ProductionLineRepository productionLineRepository;

    @MockitoBean
    private MachineRepository machineRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private RoutingRepository routingRepository;

    @MockitoBean
    private ProductionOrderRepository productionOrderRepository;

    @MockitoBean
    private WorkingCalendarRepository workingCalendarRepository;

    @MockitoBean
    private ChangeoverTimeRepository changeoverTimeRepository;

    @MockitoBean
    private MachineMaintenanceRepository machineMaintenanceRepository;

    @MockitoBean
    private ScheduleRunRepository scheduleRunRepository;

    @MockitoBean
    private ScheduleExecutionRepository scheduleExecutionRepository;

    @MockitoBean
    private ScheduledOperationRepository scheduledOperationRepository;

    @MockitoBean
    private PlanningDataImportRunRepository planningDataImportRunRepository;

    @MockitoBean
    private LearningScenarioInstanceRepository learningScenarioInstanceRepository;

    @MockitoBean
    private LearningScenarioEntityRepository learningScenarioEntityRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void servesOperationsMvpAtRootPath() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Schedule Control Tower"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "APS LEARNING LAB"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "id=\"guide-aps-map\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "class=\"guide-local-nav\" aria-label="
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "id=\"guide-scenario-grid\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "id=\"guide-rule-comparison\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "id=\"guide-constraint-impact\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "id=\"guide-frozen-horizon\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "id=\"gantt-filter-form\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "id=\"order-filter-form\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "id=\"guide-learning-progress\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "id=\"guide-result-coach\""
                        )
                ));
    }

    @Test
    void servesFrontendAsNativeEsModules() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "<script src=\"/app.js\" type=\"module\">"
                        )
                ));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "from \"./js/api.js\""
                        )
                ));

        for (String module : new String[]{
                "api", "state", "ui", "schedule-board",
                "orders", "master-data", "guide", "guide-data",
                "learning-progress"
        }) {
            mockMvc.perform(get("/js/" + module + ".js"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            "text/javascript"
                    ));
        }
    }

    @Test
    void servesUiDesignFoundation() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "class=\"nav-icon\" aria-hidden=\"true\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "class=\"button-icon\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "id=\"mobile-nav-toggle\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "id=\"view-description\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "id=\"schedule-readiness\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "class=\"plan-overview\""
                        )
                ));

        mockMvc.perform(get("/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "--radius-lg: 16px"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "--focus-ring:"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                ".nav-icon svg, .button-icon"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(
                                        "min-width: 1180px"
                                )
                        )
                ));
    }

    @Test
    void exposesHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void exposesSchedulingMetricsWithoutBusinessIdentifiers()
            throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.names",
                        hasItem("aps.schedule.execution.duration")
                ))
                .andExpect(jsonPath(
                        "$.names",
                        hasItem("aps.schedule.execution.failures")
                ));
    }
}
