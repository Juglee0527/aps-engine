package com.github.juglee0527.apsengine.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ScheduleRunController.class)
class ScheduleRunControllerTest {

    private static final OffsetDateTime PLANNING_START =
            OffsetDateTime.of(
                    2026, 7, 27, 8, 0, 0, 0,
                    ZoneOffset.ofHours(9)
            );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleRunService scheduleRunService;

    @Test
    void executesSchedule() throws Exception {
        UUID executionKey =
                UUID.fromString("3cb6bb7e-6d18-4d9b-b314-54812025c401");
        ScheduleRun scheduleRun = scheduleRun(
                executionKey,
                DispatchingRule.EDD
        );
        when(scheduleRunService.execute(
                eq(executionKey),
                any(OffsetDateTime.class),
                eq(DispatchingRule.EDD)
        )).thenReturn(scheduleRun);

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "executionKey": "3cb6bb7e-6d18-4d9b-b314-54812025c401",
                                  "planningStart": "2026-07-27T08:00:00+09:00",
                                  "dispatchingRule": "EDD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.dispatchingRule").value(
                        "EDD"
                ))
                .andExpect(jsonPath("$.totalTardinessMinutes").value(30))
                .andExpect(jsonPath("$.makespanMinutes").value(120))
                .andExpect(jsonPath("$.machineUtilizationPercent").value(
                        75.00
                ))
                .andExpect(jsonPath("$.taskCount").value(0));

        verify(scheduleRunService).execute(
                executionKey,
                OffsetDateTime.parse("2026-07-27T08:00:00+09:00"),
                DispatchingRule.EDD
        );
    }

    @Test
    void defaultsToExplicitPriorityWhenRuleIsOmitted() throws Exception {
        UUID executionKey =
                UUID.fromString("7557af3d-2134-4c04-8f54-e240e3b23dda");
        when(scheduleRunService.execute(
                eq(executionKey),
                any(OffsetDateTime.class),
                eq(DispatchingRule.EXPLICIT_PRIORITY)
        )).thenReturn(scheduleRun(executionKey));

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "executionKey": "7557af3d-2134-4c04-8f54-e240e3b23dda",
                                  "planningStart": "2026-07-27T08:00:00+09:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchingRule").value(
                        "EXPLICIT_PRIORITY"
                ));

        verify(scheduleRunService).execute(
                executionKey,
                OffsetDateTime.parse("2026-07-27T08:00:00+09:00"),
                DispatchingRule.EXPLICIT_PRIORITY
        );
    }

    @Test
    void getsLatestSchedule() throws Exception {
        when(scheduleRunService.getLatest())
                .thenReturn(scheduleRun(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/schedules/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    private ScheduleRun scheduleRun(UUID executionKey) {
        return scheduleRun(
                executionKey,
                DispatchingRule.EXPLICIT_PRIORITY
        );
    }

    private ScheduleRun scheduleRun(
            UUID executionKey,
            DispatchingRule dispatchingRule
    ) {
        ScheduleRun scheduleRun = ScheduleRun.create(
                executionKey,
                new SchedulingPlan(
                        PLANNING_START,
                        PLANNING_START,
                        List.of()
                ),
                PLANNING_START,
                dispatchingRule,
                new ScheduleKpis(
                        30,
                        1,
                        120,
                        new BigDecimal("75.00")
                )
        );
        ReflectionTestUtils.setField(scheduleRun, "id", 10L);
        return scheduleRun;
    }
}
