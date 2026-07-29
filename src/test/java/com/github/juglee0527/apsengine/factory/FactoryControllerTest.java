package com.github.juglee0527.apsengine.factory;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FactoryController.class)
class FactoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FactoryService factoryService;

    @Test
    void createsFactory() throws Exception {
        Factory factory = Factory.create("FACTORY-01", "서울 공장");
        ReflectionTestUtils.setField(factory, "id", 1L);
        when(factoryService.create("factory-01", "서울 공장"))
                .thenReturn(factory);

        mockMvc.perform(post("/api/v1/factories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "factory-01",
                                  "name": "서울 공장"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/factories/1"
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("FACTORY-01"))
                .andExpect(jsonPath("$.name").value("서울 공장"))
                .andExpect(jsonPath("$.active").value(true));

        verify(factoryService).create("factory-01", "서울 공장");
    }

    @Test
    void rejectsInvalidRequestWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/factories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "factory 01",
                                  "name": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));

        verify(factoryService, never()).create(
                "factory 01",
                " "
        );
    }
}

