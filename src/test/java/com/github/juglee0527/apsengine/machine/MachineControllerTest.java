package com.github.juglee0527.apsengine.machine;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MachineController.class)
class MachineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MachineService machineService;

    @Test
    void createsMachine() throws Exception {
        Machine machine = persistedMachine();
        when(machineService.create(
                10L,
                "machine-01",
                "절단 설비",
                MachineStatus.AVAILABLE
        ))
                .thenReturn(machine);

        mockMvc.perform(post("/api/v1/production-lines/10/machines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "machine-01",
                                  "name": "절단 설비",
                                  "status": "AVAILABLE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/machines/100"
                ))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.productionLineId").value(10))
                .andExpect(jsonPath("$.code").value("MACHINE-01"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void rejectsInvalidMachineRequest() throws Exception {
        mockMvc.perform(post("/api/v1/production-lines/10/machines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "machine 01",
                                  "name": "",
                                  "status": "AVAILABLE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));

        verify(machineService, never()).create(
                10L,
                "machine 01",
                "",
                MachineStatus.AVAILABLE
        );
    }

    @Test
    void rejectsUnknownMachineStatus() throws Exception {
        mockMvc.perform(post("/api/v1/production-lines/10/machines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "MACHINE-01",
                                  "name": "절단 설비",
                                  "status": "UNKNOWN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void getsMachineById() throws Exception {
        when(machineService.getById(100L)).thenReturn(persistedMachine());

        mockMvc.perform(get("/api/v1/machines/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.code").value("MACHINE-01"));
    }

    @Test
    void getsMachinePageByProductionLine() throws Exception {
        when(machineService.getPageByProductionLine(10L, 0, 20))
                .thenReturn(new PageImpl<>(
                        List.of(persistedMachine()),
                        PageRequest.of(0, 20),
                        1
                ));

        mockMvc.perform(get("/api/v1/production-lines/10/machines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private Machine persistedMachine() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ReflectionTestUtils.setField(factory, "id", 1L);
        ProductionLine productionLine =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        ReflectionTestUtils.setField(productionLine, "id", 10L);
        Machine machine =
                Machine.create(productionLine, "MACHINE-01", "절단 설비");
        ReflectionTestUtils.setField(machine, "id", 100L);
        return machine;
    }
}
