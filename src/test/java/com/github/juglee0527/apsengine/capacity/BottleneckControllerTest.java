package com.github.juglee0527.apsengine.capacity;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BottleneckController.class)
class BottleneckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BottleneckService bottleneckService;

    @Test
    void getsBottleneckAnalysis() throws Exception {
        OffsetDateTime from =
                OffsetDateTime.parse("2026-08-03T08:00:00+09:00");
        when(bottleneckService.detect(10L)).thenReturn(
                new BottleneckAnalysis(
                        10L,
                        from,
                        from.plusHours(8),
                        new BigDecimal("80.00"),
                        List.of(new BottleneckCandidate(
                                1,
                                20L,
                                "MACHINE-A",
                                "설비 A",
                                480,
                                420,
                                new BigDecimal("87.50"),
                                false,
                                BottleneckReason.HIGH_UTILIZATION
                        ))
                )
        );

        mockMvc.perform(get("/api/v1/schedules/10/bottlenecks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdPercent").value(80.0))
                .andExpect(jsonPath("$.candidates[0].rank").value(1))
                .andExpect(jsonPath("$.candidates[0].machineId").value(20))
                .andExpect(jsonPath(
                        "$.candidates[0].utilizationPercent"
                ).value(87.5))
                .andExpect(jsonPath(
                        "$.candidates[0].reason"
                ).value("HIGH_UTILIZATION"));
    }
}
