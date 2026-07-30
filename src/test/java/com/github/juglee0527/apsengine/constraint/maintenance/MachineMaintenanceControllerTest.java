package com.github.juglee0527.apsengine.constraint.maintenance;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

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

@WebMvcTest(MachineMaintenanceController.class)
class MachineMaintenanceControllerTest {

    private static final OffsetDateTime START =
            OffsetDateTime.parse("2026-08-03T10:00:00+09:00");
    private static final OffsetDateTime END = START.plusHours(1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MachineMaintenanceService maintenanceService;

    @Test
    void createsMaintenance() throws Exception {
        when(maintenanceService.create(
                10L,
                START,
                END,
                "정기 점검"
        )).thenReturn(persistedMaintenance());

        mockMvc.perform(post("/api/v1/machines/10/maintenances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2026-08-03T10:00:00+09:00",
                                  "endAt": "2026-08-03T11:00:00+09:00",
                                  "reason": "정기 점검"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/maintenances/100"
                ))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.machineId").value(10))
                .andExpect(jsonPath("$.reason").value("정기 점검"));
    }

    @Test
    void rejectsBlankReason() throws Exception {
        mockMvc.perform(post("/api/v1/machines/10/maintenances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2026-08-03T10:00:00+09:00",
                                  "endAt": "2026-08-03T11:00:00+09:00",
                                  "reason": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("reason"));
    }

    private MachineMaintenance persistedMaintenance() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        Machine machine =
                Machine.create(line, "MACHINE-01", "가공 설비");
        ReflectionTestUtils.setField(machine, "id", 10L);
        MachineMaintenance maintenance = MachineMaintenance.create(
                machine,
                START,
                END,
                "정기 점검"
        );
        ReflectionTestUtils.setField(maintenance, "id", 100L);
        return maintenance;
    }
}
