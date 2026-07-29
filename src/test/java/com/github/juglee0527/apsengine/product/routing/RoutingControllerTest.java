package com.github.juglee0527.apsengine.product.routing;

import static org.mockito.ArgumentMatchers.anyList;
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

@WebMvcTest(RoutingController.class)
class RoutingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoutingService routingService;

    @Test
    void createsRouting() throws Exception {
        Routing routing = persistedRouting();
        when(routingService.create(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("ROUTING-01"),
                org.mockito.ArgumentMatchers.eq("표준 Routing"),
                anyList()
        )).thenReturn(routing);

        mockMvc.perform(post("/api/v1/products/1/routings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "ROUTING-01",
                                  "name": "표준 Routing",
                                  "operations": [
                                    {
                                      "sequence": 10,
                                      "code": "CUT",
                                      "name": "절단",
                                      "processingTimeMinutes": 15,
                                      "machineId": 100
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/routings/20"
                ))
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.operations[0].machineId").value(100))
                .andExpect(jsonPath("$.operations[0].sequence").value(10));
    }

    @Test
    void rejectsEmptyOperations() throws Exception {
        mockMvc.perform(post("/api/v1/products/1/routings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "ROUTING-01",
                                  "name": "표준 Routing",
                                  "operations": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void getsRoutingsByProduct() throws Exception {
        when(routingService.getAllByProduct(1L))
                .thenReturn(List.of(persistedRouting()));

        mockMvc.perform(get("/api/v1/products/1/routings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].operations.length()").value(1));
    }

    private Routing persistedRouting() {
        Product product =
                Product.create("PRODUCT-01", "완제품 A", ProductUnit.PIECE);
        ReflectionTestUtils.setField(product, "id", 1L);
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        Machine machine = Machine.create(line, "MACHINE-01", "가공 설비");
        ReflectionTestUtils.setField(machine, "id", 100L);

        Routing routing =
                Routing.create(product, "ROUTING-01", "표준 Routing");
        ReflectionTestUtils.setField(routing, "id", 20L);
        routing.addOperation(10, "CUT", "절단", 15, machine);
        ReflectionTestUtils.setField(
                routing.operations().getFirst(),
                "id",
                200L
        );
        return routing;
    }
}
