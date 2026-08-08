package com.github.juglee0527.apsengine.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @MockitoBean
    private ScheduleExecutionService executionService;

    @MockitoBean
    private ScheduledOperationRepository scheduledOperationRepository;

    @Test
    void executesSchedule() throws Exception {
        UUID executionKey =
                UUID.fromString("3cb6bb7e-6d18-4d9b-b314-54812025c401");
        ScheduleExecutionResponse execution = queuedExecution(
                executionKey,
                DispatchingRule.EDD
        );
        when(executionService.submit(
                eq(executionKey),
                any(OffsetDateTime.class),
                eq(DispatchingRule.EDD),
                isNull()
        )).thenReturn(execution);

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "executionKey": "3cb6bb7e-6d18-4d9b-b314-54812025c401",
                                  "planningStart": "2026-07-27T08:00:00+09:00",
                                  "dispatchingRule": "EDD"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/schedules/executions/31"
                ))
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.dispatchingRule").value(
                        "EDD"
                ))
                .andExpect(jsonPath("$.resultScheduleRunId").isEmpty());

        verify(executionService).submit(
                executionKey,
                OffsetDateTime.parse("2026-07-27T08:00:00+09:00"),
                DispatchingRule.EDD,
                null
        );
    }

    @Test
    void defaultsToExplicitPriorityWhenRuleIsOmitted() throws Exception {
        UUID executionKey =
                UUID.fromString("7557af3d-2134-4c04-8f54-e240e3b23dda");
        when(executionService.submit(
                eq(executionKey),
                any(OffsetDateTime.class),
                eq(DispatchingRule.EXPLICIT_PRIORITY),
                isNull()
        )).thenReturn(queuedExecution(executionKey));

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "executionKey": "7557af3d-2134-4c04-8f54-e240e3b23dda",
                                  "planningStart": "2026-07-27T08:00:00+09:00"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.dispatchingRule").value(
                        "EXPLICIT_PRIORITY"
                ));

        verify(executionService).submit(
                executionKey,
                OffsetDateTime.parse("2026-07-27T08:00:00+09:00"),
                DispatchingRule.EXPLICIT_PRIORITY,
                null
        );
    }

    @Test
    void acceptsExplicitProductionOrderScope() throws Exception {
        UUID executionKey =
                UUID.fromString("e64a3935-28a0-45db-8214-a31cbf846bc1");
        ScheduleExecutionResponse execution = queuedExecution(executionKey);
        when(executionService.submit(
                eq(executionKey),
                any(OffsetDateTime.class),
                eq(DispatchingRule.EXPLICIT_PRIORITY),
                eq(List.of(12L, 7L, 12L))
        )).thenReturn(execution);

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "executionKey": "e64a3935-28a0-45db-8214-a31cbf846bc1",
                                  "planningStart": "2026-07-27T08:00:00+09:00",
                                  "productionOrderIds": [12, 7, 12]
                                }
                                """))
                .andExpect(status().isAccepted());

        verify(executionService).submit(
                executionKey,
                OffsetDateTime.parse("2026-07-27T08:00:00+09:00"),
                DispatchingRule.EXPLICIT_PRIORITY,
                List.of(12L, 7L, 12L)
        );
    }

    @Test
    void reschedulesFromExistingRunWithFrozenHorizon() throws Exception {
        UUID executionKey =
                UUID.fromString("8743f2eb-5b06-43f8-ac75-31f0d43aaf0c");
        OffsetDateTime frozenAt = PLANNING_START.plusHours(1);
        ScheduleExecutionResponse result = queuedReschedule(
                executionKey,
                frozenAt,
                DispatchingRule.SPT
        );
        when(executionService.submitReschedule(
                9L,
                executionKey,
                frozenAt,
                DispatchingRule.SPT
        )).thenReturn(result);

        mockMvc.perform(post("/api/v1/schedules/9/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "executionKey": "8743f2eb-5b06-43f8-ac75-31f0d43aaf0c",
                                  "frozenAt": "2026-07-27T09:00:00+09:00",
                                  "dispatchingRule": "SPT"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sourceScheduleRunId").value(9))
                .andExpect(jsonPath("$.frozenAt").value(
                        "2026-07-27T09:00:00+09:00"
                ))
                .andExpect(jsonPath("$.dispatchingRule").value("SPT"));
    }

    @Test
    void getsExecutionStatusAndRecentHistory() throws Exception {
        ScheduleExecutionResponse execution =
                queuedExecution(UUID.randomUUID());
        when(executionService.find(31L)).thenReturn(execution);
        when(executionService.findRecent(10))
                .thenReturn(List.of(execution));

        mockMvc.perform(get("/api/v1/schedules/executions/31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.status").value("QUEUED"));

        mockMvc.perform(get("/api/v1/schedules/executions")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(31));
    }

    @Test
    void getsLatestSchedule() throws Exception {
        when(scheduleRunService.getLatest())
                .thenReturn(scheduleRun(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/schedules/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getsLatestSummaryWithoutEmbeddingAllTasks() throws Exception {
        ScheduleRun run = scheduleRun(UUID.randomUUID());
        when(scheduleRunService.getLatestSummary()).thenReturn(run);
        when(scheduledOperationRepository.countOrders(10L)).thenReturn(150L);
        when(scheduledOperationRepository.countByScheduleRun_Id(10L))
                .thenReturn(600L);

        mockMvc.perform(get("/api/v1/schedules/latest/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.orderCount").value(150))
                .andExpect(jsonPath("$.taskCount").value(600))
                .andExpect(jsonPath("$.tasks").doesNotExist());
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

    private ScheduleExecutionResponse queuedExecution(
            UUID executionKey
    ) {
        return queuedExecution(
                executionKey,
                DispatchingRule.EXPLICIT_PRIORITY
        );
    }

    private ScheduleExecutionResponse queuedExecution(
            UUID executionKey,
            DispatchingRule dispatchingRule
    ) {
        return new ScheduleExecutionResponse(
                31L,
                executionKey,
                ScheduleExecutionStatus.QUEUED,
                PLANNING_START,
                PLANNING_START.getOffset().getTotalSeconds(),
                dispatchingRule,
                null,
                null,
                null,
                null,
                PLANNING_START,
                null,
                null
        );
    }

    private ScheduleExecutionResponse queuedReschedule(
            UUID executionKey,
            OffsetDateTime frozenAt,
            DispatchingRule dispatchingRule
    ) {
        return new ScheduleExecutionResponse(
                31L,
                executionKey,
                ScheduleExecutionStatus.QUEUED,
                PLANNING_START,
                PLANNING_START.getOffset().getTotalSeconds(),
                dispatchingRule,
                9L,
                frozenAt,
                null,
                null,
                PLANNING_START,
                null,
                null
        );
    }
}
