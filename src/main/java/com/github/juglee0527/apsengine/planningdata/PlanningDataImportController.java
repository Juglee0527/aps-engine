package com.github.juglee0527.apsengine.planningdata;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/planning-data/imports")
public class PlanningDataImportController {

    private final PlanningDataImportPreviewService previewService;
    private final PlanningDataImportService importService;

    public PlanningDataImportController(
            PlanningDataImportPreviewService previewService,
            PlanningDataImportService importService
    ) {
        this.previewService = previewService;
        this.importService = importService;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PlanningDataImportRunResponse execute(
            @RequestParam UUID requestKey,
            @RequestPart(name = "file", required = false) MultipartFile file
    ) {
        return importService.execute(requestKey, file);
    }

    @GetMapping("/{importRunId}")
    public PlanningDataImportRunResponse find(
            @PathVariable Long importRunId
    ) {
        return importService.find(importRunId);
    }
}
