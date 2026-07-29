package com.github.juglee0527.apsengine.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.product.Product;
import com.github.juglee0527.apsengine.product.ProductUnit;
import com.github.juglee0527.apsengine.product.routing.Routing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductionOrderController.class)
class ProductionOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductionOrderService productionOrderService;

    @Test
    void createsProductionOrder() throws Exception {
        ProductionOrder order = persistedOrder();
        when(productionOrderService.create(
                eq("PO-001"),
                eq(20L),
                eq(10L),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                eq(80)
        )).thenReturn(order);

        mockMvc.perform(post("/api/v1/production-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderNumber": "PO-001",
                                  "routingId": 20,
                                  "quantity": 10,
                                  "releaseAt": "2026-08-03T08:00:00+09:00",
                                  "dueAt": "2026-08-04T18:00:00+09:00",
                                  "priority": 80
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/production-orders/30"
                ))
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.routingId").value(20))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void confirmsProductionOrder() throws Exception {
        ProductionOrder order = persistedOrder();
        order.confirm();
        when(productionOrderService.confirm(30L)).thenReturn(order);

        mockMvc.perform(post("/api/v1/production-orders/30/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getsProductionOrderPage() throws Exception {
        ProductionOrder order = persistedOrder();
        when(productionOrderService.getPage(0, 20))
                .thenReturn(new PageImpl<>(
                        List.of(order),
                        PageRequest.of(0, 20),
                        1
                ));

        mockMvc.perform(get("/api/v1/production-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber")
                        .value("PO-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private ProductionOrder persistedOrder() {
        Product product =
                Product.create("PRODUCT-01", "완제품 A", ProductUnit.PIECE);
        ReflectionTestUtils.setField(product, "id", 1L);
        Routing routing =
                Routing.create(product, "ROUTING-01", "표준 Routing");
        ReflectionTestUtils.setField(routing, "id", 20L);
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ProductionLine line =
                ProductionLine.create(factory, "LINE-01", "조립 라인");
        Machine machine = Machine.create(line, "MACHINE-01", "가공 설비");
        routing.addOperation(10, "CUT", "절단", 15, machine);
        ProductionOrder order = ProductionOrder.create(
                routing,
                "PO-001",
                10,
                OffsetDateTime.parse("2026-08-03T08:00:00+09:00"),
                OffsetDateTime.parse("2026-08-04T18:00:00+09:00"),
                80
        );
        ReflectionTestUtils.setField(order, "id", 30L);
        return order;
    }
}
