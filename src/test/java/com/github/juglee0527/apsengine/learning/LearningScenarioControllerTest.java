package com.github.juglee0527.apsengine.learning;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LearningScenarioController.class)
class LearningScenarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LearningScenarioService service;

    @Test
    void listsScenarioDefinitions() throws Exception {
        when(service.findScenarios()).thenReturn(List.of(
                new LearningScenarioDefinition(
                        "FIRST_PLAN", "A", "첫 생산계획", "설명", 3, 3, 8
                )
        ));

        mockMvc.perform(get("/api/v1/learning/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("FIRST_PLAN"))
                .andExpect(jsonPath("$[0].expectedOrderCount").value(8));
    }

    @Test
    void createsAndResetsScenarioInstance() throws Exception {
        UUID requestKey = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.parse(
                "2026-08-10T08:00:00+09:00"
        );
        when(service.create(eq("FIRST_PLAN"), eq(requestKey)))
                .thenReturn(response(4L, requestKey, LearningScenarioStatus.READY, now));
        when(service.reset(4L)).thenReturn(response(
                4L, requestKey, LearningScenarioStatus.RESET, now
        ));

        mockMvc.perform(post(
                        "/api/v1/learning/scenarios/FIRST_PLAN/instances"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestKey\":\"" + requestKey + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/learning/instances/4"
                ))
                .andExpect(jsonPath("$.status").value("READY"));

        mockMvc.perform(delete("/api/v1/learning/instances/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESET"));
    }

    private LearningScenarioInstanceResponse response(
            long id,
            UUID requestKey,
            LearningScenarioStatus status,
            OffsetDateTime now
    ) {
        return new LearningScenarioInstanceResponse(
                id,
                requestKey,
                "FIRST_PLAN",
                "LEARN-TEST",
                status,
                now,
                now,
                0
        );
    }
}
