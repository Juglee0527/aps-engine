package com.github.juglee0527.apsengine.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        ScheduleRun scheduleRun = scheduleRun(executionKey);
        when(scheduleRunService.execute(
                eq(executionKey),
                any(OffsetDateTime.class)
        )).thenReturn(scheduleRun);

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "executionKey": "3cb6bb7e-6d18-4d9b-b314-54812025c401",
                                  "planningStart": "2026-07-27T08:00:00+09:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.taskCount").value(0));

        verify(scheduleRunService).execute(
                executionKey,
                OffsetDateTime.parse("2026-07-27T08:00:00+09:00")
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
        ScheduleRun scheduleRun = ScheduleRun.create(
                executionKey,
                new SchedulingPlan(
                        PLANNING_START,
                        PLANNING_START,
                        List.of()
                ),
                PLANNING_START
        );
        ReflectionTestUtils.setField(scheduleRun, "id", 10L);
        return scheduleRun;
    }
}
