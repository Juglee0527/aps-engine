package com.github.juglee0527.apsengine.planningdata;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlanningDataImportController.class)
class PlanningDataImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanningDataImportPreviewService previewService;

    @Test
    void previewsMultipartCsvFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "planning-data.csv",
                "text/csv",
                "type\nFACTORY".getBytes(StandardCharsets.UTF_8)
        );
        when(previewService.preview(any())).thenReturn(
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
}
