
package com.charron.api.loading.api;

import com.charron.api.loading.application.LoadingPlanService;
import com.charron.api.loading.domain.LoadingPlan;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
public class LoadingPlanController {

    private final LoadingPlanService service;

    public LoadingPlanController(LoadingPlanService service) {
        this.service = Objects.requireNonNull(service);
    }

    public record CreateLoadingPlanRequest(
            UUID equipmentId,
            UUID cargoSpaceId
    ) {}

    @PostMapping("/api/movements/{movementId}/loading-plans")
    @ResponseStatus(HttpStatus.CREATED)
    public LoadingPlan create(
            @PathVariable UUID movementId,
            @RequestBody CreateLoadingPlanRequest request
    ) {
        Objects.requireNonNull(request, "Request is required");

        return service.create(
                movementId,
                request.equipmentId(),
                request.cargoSpaceId()
        );
    }

    @GetMapping("/api/movements/{movementId}/loading-plans")
    public List<LoadingPlan> listByMovement(
            @PathVariable UUID movementId
    ) {
        return service.getByMovementId(movementId);
    }

    @GetMapping("/api/loading-plans/{planId}")
    public LoadingPlan getById(
            @PathVariable UUID planId
    ) {
        return service.getById(planId);
    }
}
