package com.github.juglee0527.apsengine.factory.line;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.juglee0527.apsengine.factory.Factory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductionLineController.class)
class ProductionLineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductionLineService productionLineService;

    @Test
    void createsProductionLine() throws Exception {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ReflectionTestUtils.setField(factory, "id", 1L);
        ProductionLine productionLine = ProductionLine.create(
                factory,
                "LINE-01",
                "조립 라인"
        );
        ReflectionTestUtils.setField(productionLine, "id", 10L);
        when(productionLineService.create(1L, "line-01", "조립 라인"))
                .thenReturn(productionLine);

        mockMvc.perform(post("/api/v1/factories/1/production-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "line-01",
                                  "name": "조립 라인"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/factories/1/production-lines/10"
                ))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.factoryId").value(1))
                .andExpect(jsonPath("$.code").value("LINE-01"))
                .andExpect(jsonPath("$.name").value("조립 라인"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/factories/1/production-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "line 01",
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));

        verify(productionLineService, never()).create(
                1L,
                "line 01",
                ""
        );
    }
}

