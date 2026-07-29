package com.github.juglee0527.apsengine.capacity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkingCalendarController.class)
class WorkingCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkingCalendarService workingCalendarService;

    @Test
    void createsWorkingCalendar() throws Exception {
        when(workingCalendarService.create(eq(1L), any()))
                .thenReturn(List.of(persistedCalendar()));

        mockMvc.perform(post("/api/v1/machines/1/working-calendars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entries": [
                                    {
                                      "dayOfWeek": "MONDAY",
                                      "startTime": "08:00:00",
                                      "endTime": "17:00:00"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].machineId").value(1))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].startTime").value("08:00:00"));
    }

    @Test
    void getsMachineAvailability() throws Exception {
        OffsetDateTime from =
                OffsetDateTime.parse("2026-08-03T08:00:00+09:00");
        OffsetDateTime to =
                OffsetDateTime.parse("2026-08-03T17:00:00+09:00");
        when(workingCalendarService.getAvailability(
                eq(1L),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(new MachineAvailabilityResponse(
                1L,
                from,
                to,
                540,
                List.of(new AvailabilityInterval(from, to))
        ));

        mockMvc.perform(get("/api/v1/machines/1/availability")
                        .queryParam(
                                "from",
                                "2026-08-03T08:00:00+09:00"
                        )
                        .queryParam(
                                "to",
                                "2026-08-03T17:00:00+09:00"
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableMinutes").value(540))
                .andExpect(jsonPath("$.intervals.length()").value(1));
    }

    private WorkingCalendar persistedCalendar() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        Machine machine = Machine.create(line, "MACHINE-01", "가공 설비");
        ReflectionTestUtils.setField(machine, "id", 1L);
        WorkingCalendar calendar = WorkingCalendar.create(
                machine,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0)
        );
        ReflectionTestUtils.setField(calendar, "id", 10L);
        return calendar;
    }
}
