package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanCargo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class LoadingPlanCargoService {

    private final LoadingPlanCargoRepository cargoRepository;
    private final LoadingPlanService loadingPlanService;
    private final LoadingMovementLock movementLock;

    public LoadingPlanCargoService(
            LoadingPlanCargoRepository cargoRepository,
            LoadingPlanService loadingPlanService,
            LoadingMovementLock movementLock
    ) {

        this.cargoRepository =
                Objects.requireNonNull(cargoRepository);

        this.loadingPlanService =
                Objects.requireNonNull(loadingPlanService);

        this.movementLock =
                Objects.requireNonNull(movementLock);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LoadingPlanCargo addCargo(
            UUID loadingPlanId,
            UUID cargoId,
            int quantity
    ) {

        requireId(loadingPlanId, "Loading plan");
        requireId(cargoId, "Cargo");
        validateQuantity(quantity);

        lockParentMovement(loadingPlanId);

        if (cargoRepository.existsByLoadingPlanAndCargo(
                loadingPlanId,
                cargoId
        )) {

            throw new LoadingPlanConflictException(
                    "Cargo is already assigned to this loading plan"
            );
        }

        LoadingPlanCargo cargoLine =
                LoadingPlanCargo.create(
                        loadingPlanId,
                        cargoId,
                        quantity
                );

        cargoRepository.insert(cargoLine);

        return cargoLine;
    }

    @Transactional(readOnly = true)
    public List<LoadingPlanCargo> getCargo(
            UUID loadingPlanId
    ) {

        requireId(loadingPlanId, "Loading plan");

        loadingPlanService.getById(loadingPlanId);

        return cargoRepository.findByLoadingPlanId(
                loadingPlanId
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LoadingPlanCargo updateQuantity(
            UUID loadingPlanId,
            UUID cargoLineId,
            int quantity
    ) {

        requireId(loadingPlanId, "Loading plan");
        requireId(cargoLineId, "Cargo line");
        validateQuantity(quantity);

        lockParentMovement(loadingPlanId);

        LoadingPlanCargo existing = findCargoLine(
                loadingPlanId,
                cargoLineId
        );

        LoadingPlanCargo updated =
                existing.withQuantity(quantity);

        cargoRepository.updateQuantity(updated);

        return updated;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeCargo(
            UUID loadingPlanId,
            UUID cargoLineId
    ) {

        requireId(loadingPlanId, "Loading plan");
        requireId(cargoLineId, "Cargo line");

        lockParentMovement(loadingPlanId);

        findCargoLine(
                loadingPlanId,
                cargoLineId
        );

        cargoRepository.deleteById(cargoLineId);
    }

    private void lockParentMovement(UUID loadingPlanId) {

        LoadingPlan plan =
                loadingPlanService.getById(loadingPlanId);

        if (!movementLock.acquire(plan.movementId())) {

            throw new LoadingPlanNotFoundException(
                    "Movement does not exist: "
                            + plan.movementId()
            );
        }
    }

    private LoadingPlanCargo findCargoLine(
            UUID loadingPlanId,
            UUID cargoLineId
    ) {

        LoadingPlanCargo cargoLine = cargoRepository
                .findById(cargoLineId)
                .orElseThrow(
                        () -> new LoadingPlanNotFoundException(
                                "Cargo line does not exist: "
                                        + cargoLineId
                        )
                );

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