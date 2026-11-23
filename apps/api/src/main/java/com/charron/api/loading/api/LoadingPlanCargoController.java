
package com.charron.api.loading.api;

import com.charron.api.loading.application.InvalidLoadingPlanException;
import com.charron.api.loading.application.LoadingPlanCargoService;
import com.charron.api.loading.domain.LoadingPlanCargo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/loading-plans/{planId}/cargo")
public class LoadingPlanCargoController {

    private final LoadingPlanCargoService service;

    public LoadingPlanCargoController(
            LoadingPlanCargoService service
    ) {
        this.service = Objects.requireNonNull(service);
    }

    public record AddCargoRequest(
            UUID cargoId,
            Integer quantity
    ) {}

    public record UpdateQuantityRequest(
            Integer quantity
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoadingPlanCargo addCargo(
            @PathVariable UUID planId,
            @RequestBody AddCargoRequest request
    ) {
        if (request == null
                || request.cargoId() == null
                || request.quantity() == null) {
            throw new InvalidLoadingPlanException(
                    "Cargo ID and quantity are required"
            );
        }

        return service.addCargo(
                planId,
                request.cargoId(),
                request.quantity()
        );
    }

    @GetMapping
    public List<LoadingPlanCargo> getCargo(
            @PathVariable UUID planId
    ) {
        return service.getCargo(planId);
    }

    @PutMapping("/{cargoLineId}")
    public LoadingPlanCargo updateQuantity(
            @PathVariable UUID planId,
            @PathVariable UUID cargoLineId,
            @RequestBody UpdateQuantityRequest request
    ) {
        if (request == null || request.quantity() == null) {
            throw new InvalidLoadingPlanException(
                    "Cargo quantity is required"
            );
        }

        return service.updateQuantity(
                planId,
                cargoLineId,
                request.quantity()
        );
    }

    @DeleteMapping("/{cargoLineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCargo(
            @PathVariable UUID planId,
            @PathVariable UUID cargoLineId
    ) {
        service.removeCargo(planId, cargoLineId);
    }
}
