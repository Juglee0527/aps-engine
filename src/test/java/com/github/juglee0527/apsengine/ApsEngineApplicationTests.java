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
import com.github.juglee0527.apsengine.order.ProductionOrderRepository;
import com.github.juglee0527.apsengine.product.ProductRepository;
import com.github.juglee0527.apsengine.product.routing.RoutingRepository;
import com.github.juglee0527.apsengine.planningdata.PlanningDataImportRunRepository;
import com.github.juglee0527.apsengine.scheduling.ScheduleRunRepository;
import com.github.juglee0527.apsengine.scheduling.ScheduleExecutionRepository;

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
    private PlanningDataImportRunRepository planningDataImportRunRepository;

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
