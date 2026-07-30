package com.github.juglee0527.apsengine.scheduling;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlannedLeadTimeController.class)
class PlannedLeadTimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlannedLeadTimeService plannedLeadTimeService;

    @Test
    void getsPlannedLeadTimes() throws Exception {
        OffsetDateTime releaseAt =
                OffsetDateTime.parse("2026-08-03T08:00:00+09:00");
        when(plannedLeadTimeService.calculate(10L))
                .thenReturn(List.of(new PlannedLeadTime(
                        20L,
                        "PO-001",
                        30L,
                        "PRODUCT-A",
                        releaseAt,
                        releaseAt.plusHours(5),
                        300,
                        120,
                        30,
                        150,
                        2
                )));

        mockMvc.perform(get("/api/v1/schedules/10/lead-times"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productionOrderId").value(20))
                .andExpect(jsonPath("$[0].plannedLeadTimeMinutes")
                        .value(300))
                .andExpect(jsonPath("$[0].processingMinutes").value(120))
                .andExpect(jsonPath("$[0].changeoverMinutes").value(30))
                .andExpect(jsonPath("$[0].waitingMinutes").value(150))
                .andExpect(jsonPath("$[0].operationCount").value(2));
    }
}
