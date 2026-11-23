
package com.charron.api.loading.api;

import com.charron.api.loading.application.LoadingPlanArrangementGenerationService;
import com.charron.api.loading.application.LoadingPlanArrangementService;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/loading-plans/{planId}/arrangement")
public class LoadingPlanArrangementController {

    private final LoadingPlanArrangementGenerationService generationService;
    private final LoadingPlanArrangementService arrangementService;

    public LoadingPlanArrangementController(
            LoadingPlanArrangementGenerationService generationService,
            LoadingPlanArrangementService arrangementService
    ) {
        this.generationService = Objects.requireNonNull(generationService);
        this.arrangementService = Objects.requireNonNull(arrangementService);
    }

    @PostMapping("/generate")
    public LoadingPlanArrangement generateAndSave(
            @PathVariable UUID planId
    ) {
        return generationService.generateAndSave(planId);
    }

    @GetMapping
    public ResponseEntity<LoadingPlanArrangement> getSavedArrangement(
            @PathVariable UUID planId
    ) {
        return arrangementService.findByLoadingPlanId(planId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
