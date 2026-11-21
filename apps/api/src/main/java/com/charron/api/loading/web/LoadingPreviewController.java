
package com.charron.api.loading.web;

import com.charron.api.loading.application.LoadingPreviewService;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/loading")
public class LoadingPreviewController {

    private final LoadingPreviewService previewService;

    public LoadingPreviewController(
            LoadingPreviewService previewService
    ) {
        this.previewService = Objects.requireNonNull(
                previewService,
                "Loading preview service is required"
        );
    }

    @PostMapping("/preview")
    public LoadingPreviewResponse preview(
            @RequestBody LoadingPreviewRequest request
    ) {
        return previewService.preview(request);
    }
}
