package com.github.juglee0527.apsengine.planningdata;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/planning-data/imports")
public class PlanningDataImportController {

    private final PlanningDataImportPreviewService previewService;

    public PlanningDataImportController(
            PlanningDataImportPreviewService previewService
    ) {
        this.previewService = previewService;
    }

    @PostMapping(
            path = "/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public PlanningDataImportPreviewResponse preview(
            @RequestPart(name = "file", required = false) MultipartFile file
    ) {
        return previewService.preview(file);
    }
}
