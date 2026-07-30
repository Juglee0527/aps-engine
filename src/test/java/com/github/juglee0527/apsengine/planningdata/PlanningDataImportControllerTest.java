package com.github.juglee0527.apsengine.planningdata;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(PlanningDataImportController.class)
class PlanningDataImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanningDataImportPreviewService previewService;

    @MockitoBean
    private PlanningDataImportService importService;

    @Test
    void previewsMultipartCsvFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "planning-data.csv",
                "text/csv",
                "type\nFACTORY".getBytes(StandardCharsets.UTF_8)
        );
        when(previewService.preview(any(MultipartFile.class))).thenReturn(
                new PlanningDataImportPreviewResponse(
                        false,
                        1,
                        0,
                        1,
                        List.of(new PlanningDataImportRowPreview(
                                2,
                                "FACTORY",
                                false,
                                Map.of(),
                                List.of(new PlanningDataImportRowError(
                                        "factoryCode",
                                        "REQUIRED",
                                        "factoryCode은(는) 필수입니다."
                                ))
                        ))
                )
        );

        mockMvc.perform(multipart(
                        "/api/v1/planning-data/imports/preview"
                ).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readyToApply").value(false))
                .andExpect(jsonPath("$.invalidRows").value(1))
                .andExpect(jsonPath("$.rows[0].rowNumber").value(2))
                .andExpect(jsonPath("$.rows[0].errors[0].code")
                        .value("REQUIRED"));
    }

    @Test
    void appliesValidatedCsvWithRequestKey() throws Exception {
        UUID requestKey = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "planning-data.csv",
                "text/csv",
                "type\nFACTORY".getBytes(StandardCharsets.UTF_8)
        );
        when(importService.execute(eq(requestKey), any()))
                .thenReturn(completedRun(requestKey));

        mockMvc.perform(multipart("/api/v1/planning-data/imports")
                        .file(file)
                        .param("requestKey", requestKey.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestKey")
                        .value(requestKey.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.successRows").value(1));
    }

    @Test
    void rejectsApplyWithoutRequestKey() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "planning-data.csv",
                "text/csv",
                "type\nFACTORY".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/planning-data/imports")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("requestKey"));
    }

    @Test
    void findsImportRunById() throws Exception {
        UUID requestKey = UUID.randomUUID();
        when(importService.find(41L))
                .thenReturn(completedRun(requestKey));

        mockMvc.perform(get("/api/v1/planning-data/imports/41"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(41))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private PlanningDataImportRunResponse completedRun(UUID requestKey) {
        OffsetDateTime now =
                OffsetDateTime.parse("2026-07-30T10:00:00+09:00");
        return new PlanningDataImportRunResponse(
                41L,
                requestKey,
                "planning-data.csv",
                "a".repeat(64),
                PlanningDataImportStatus.COMPLETED,
                1,
                1,
                0,
                0,
                0,
                null,
                now,
                now,
                now,
                List.of()
        );
    }
}
