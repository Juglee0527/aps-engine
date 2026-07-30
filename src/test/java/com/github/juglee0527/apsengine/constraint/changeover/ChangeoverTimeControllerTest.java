package com.github.juglee0527.apsengine.constraint.changeover;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChangeoverTimeController.class)
class ChangeoverTimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChangeoverTimeService changeoverTimeService;

    @Test
    void createsChangeoverTime() throws Exception {
        ChangeoverTime changeoverTime = persistedChangeoverTime();
        when(changeoverTimeService.create(100L, 10L, 20L, 30))
                .thenReturn(changeoverTime);

        mockMvc.perform(post("/api/v1/machines/100/changeover-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromProductId": 10,
                                  "toProductId": 20,
                                  "changeoverMinutes": 30
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/changeover-times/200"
                ))
                .andExpect(jsonPath("$.id").value(200))
                .andExpect(jsonPath("$.machineId").value(100))
                .andExpect(jsonPath("$.fromProductCode").value("PRODUCT-A"))
                .andExpect(jsonPath("$.toProductCode").value("PRODUCT-B"))
                .andExpect(jsonPath("$.changeoverMinutes").value(30));
    }

    @Test
    void rejectsNegativeChangeoverMinutes() throws Exception {
        mockMvc.perform(post("/api/v1/machines/100/changeover-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromProductId": 10,
                                  "toProductId": 20,
                                  "changeoverMinutes": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("changeoverMinutes"));
    }

    @Test
    void getsChangeoverTimesByMachine() throws Exception {
        when(changeoverTimeService.getAllByMachine(100L))
                .thenReturn(List.of(persistedChangeoverTime()));

        mockMvc.perform(get("/api/v1/machines/100/changeover-times"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].machineCode").value("MACHINE-01"))
                .andExpect(jsonPath("$[0].fromProductId").value(10))
                .andExpect(jsonPath("$[0].toProductId").value(20));
    }

    @Test
    void getsChangeoverTimeById() throws Exception {
        when(changeoverTimeService.getById(200L))
                .thenReturn(persistedChangeoverTime());

        mockMvc.perform(get("/api/v1/changeover-times/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200))
                .andExpect(jsonPath("$.machineId").value(100))
                .andExpect(jsonPath("$.changeoverMinutes").value(30));
    }

    private ChangeoverTime persistedChangeoverTime() {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        Machine machine = Machine.create(line, "MACHINE-01", "가공 설비");
        ReflectionTestUtils.setField(machine, "id", 100L);
        Product fromProduct =
                Product.create("PRODUCT-A", "제품 A", ProductUnit.PIECE);
        ReflectionTestUtils.setField(fromProduct, "id", 10L);
        Product toProduct =
                Product.create("PRODUCT-B", "제품 B", ProductUnit.PIECE);
        ReflectionTestUtils.setField(toProduct, "id", 20L);
        ChangeoverTime changeoverTime =
                ChangeoverTime.create(machine, fromProduct, toProduct, 30);
        ReflectionTestUtils.setField(changeoverTime, "id", 200L);
        return changeoverTime;
    }
}
