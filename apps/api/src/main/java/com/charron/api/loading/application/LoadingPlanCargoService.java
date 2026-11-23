
package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlanCargo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class LoadingPlanCargoService {

    private final LoadingPlanCargoRepository cargoRepository;
    private final LoadingPlanService loadingPlanService;

    public LoadingPlanCargoService(
            LoadingPlanCargoRepository cargoRepository,
            LoadingPlanService loadingPlanService
    ) {
        this.cargoRepository = Objects.requireNonNull(cargoRepository);
        this.loadingPlanService = Objects.requireNonNull(loadingPlanService);
    }

    @Transactional
    public LoadingPlanCargo addCargo(
            UUID loadingPlanId,
            UUID cargoId,
            int quantity
    ) {
        requireId(loadingPlanId, "Loading plan");
        requireId(cargoId, "Cargo");
        validateQuantity(quantity);

        loadingPlanService.getById(loadingPlanId);

        if (cargoRepository.existsByLoadingPlanAndCargo(
                loadingPlanId,
                cargoId
        )) {
            throw new LoadingPlanConflictException(
                    "Cargo is already assigned to this loading plan"
            );
        }

        LoadingPlanCargo cargoLine = LoadingPlanCargo.create(
                loadingPlanId,
                cargoId,
                quantity
        );

        // The cargo foreign key protects against unknown catalog IDs.
        cargoRepository.insert(cargoLine);

        return cargoLine;
    }

    @Transactional(readOnly = true)
    public List<LoadingPlanCargo> getCargo(
            UUID loadingPlanId
    ) {
        requireId(loadingPlanId, "Loading plan");

        loadingPlanService.getById(loadingPlanId);

        return cargoRepository.findByLoadingPlanId(loadingPlanId);
    }

    @Transactional
    public LoadingPlanCargo updateQuantity(
            UUID loadingPlanId,
            UUID cargoLineId,
            int quantity
    ) {
        requireId(loadingPlanId, "Loading plan");
        requireId(cargoLineId, "Cargo line");
        validateQuantity(quantity);

        loadingPlanService.getById(loadingPlanId);

        LoadingPlanCargo existing = findCargoLine(
                loadingPlanId,
                cargoLineId
        );

        LoadingPlanCargo updated = existing.withQuantity(quantity);

        cargoRepository.updateQuantity(updated);

        return updated;
    }

    @Transactional
    public void removeCargo(
            UUID loadingPlanId,
            UUID cargoLineId
    ) {
        requireId(loadingPlanId, "Loading plan");
        requireId(cargoLineId, "Cargo line");

        loadingPlanService.getById(loadingPlanId);

        findCargoLine(loadingPlanId, cargoLineId);

        cargoRepository.deleteById(cargoLineId);
    }

    private LoadingPlanCargo findCargoLine(
            UUID loadingPlanId,
            UUID cargoLineId
    ) {
        LoadingPlanCargo cargoLine = cargoRepository
                .findById(cargoLineId)
                .orElseThrow(() -> new LoadingPlanNotFoundException(
                        "Cargo line does not exist: " + cargoLineId
                ));

        if (!cargoLine.loadingPlanId().equals(loadingPlanId)) {
            throw new LoadingPlanNotFoundException(
                    "Cargo line does not belong to loading plan: "
                            + loadingPlanId
            );
        }

        return cargoLine;
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidLoadingPlanException(
                    "Cargo quantity must be greater than zero"
            );
        }
    }

    private void requireId(UUID id, String field) {
        if (id == null) {
            throw new InvalidLoadingPlanException(
                    field + " ID is required"
            );
        }
    }
}
