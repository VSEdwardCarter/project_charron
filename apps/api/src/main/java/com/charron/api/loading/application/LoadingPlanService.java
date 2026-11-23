
package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlan;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class LoadingPlanService {

    private final LoadingPlanRepository repository;
    private final CargoSpaceOwnershipChecker cargoSpaceOwnershipChecker;
    private final LoadingPlanEligibilityChecker eligibilityChecker;

    public LoadingPlanService(
            LoadingPlanRepository repository,
            CargoSpaceOwnershipChecker cargoSpaceOwnershipChecker,
            LoadingPlanEligibilityChecker eligibilityChecker
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.cargoSpaceOwnershipChecker =
                Objects.requireNonNull(cargoSpaceOwnershipChecker);
        this.eligibilityChecker =
                Objects.requireNonNull(eligibilityChecker);
    }

    @Transactional
    public LoadingPlan create(
            UUID movementId,
            UUID equipmentId,
            UUID cargoSpaceId
    ) {
        if (movementId == null || equipmentId == null
                || cargoSpaceId == null) {
            throw new InvalidLoadingPlanException(
                    "Movement, equipment, and cargo space are required"
            );
        }

        if (!eligibilityChecker.movementExists(movementId)) {
            throw new LoadingPlanNotFoundException(
                    "Movement does not exist: " + movementId
            );
        }

        if (!eligibilityChecker.equipmentAssignedToMovement(
                movementId, equipmentId
        )) {
            throw new LoadingPlanConflictException(
                    "Equipment is not assigned to the movement"
            );
        }

        if (!cargoSpaceOwnershipChecker.belongsToEquipment(
                cargoSpaceId, equipmentId
        )) {
            throw new InvalidLoadingPlanException(
                    "Cargo space does not belong to the selected equipment"
            );
        }

        if (repository.existsByMovementAndCargoSpace(
                movementId, cargoSpaceId
        )) {
            throw new LoadingPlanConflictException(
                    "A loading plan already exists for this "
                            + "movement and cargo space"
            );
        }

        LoadingPlan plan = LoadingPlan.create(
                movementId,
                equipmentId,
                cargoSpaceId
        );

        try {
            repository.insert(plan);
        } catch (DataIntegrityViolationException exception) {
            throw exception;
        }

        return plan;
    }

    @Transactional(readOnly = true)
    public LoadingPlan getById(UUID planId) {
        Objects.requireNonNull(planId, "Loading plan ID is required");

        return repository.findById(planId)
                .orElseThrow(() -> new LoadingPlanNotFoundException(
                        "Loading plan does not exist: " + planId
                ));
    }

    @Transactional(readOnly = true)
    public List<LoadingPlan> getByMovementId(UUID movementId) {
        Objects.requireNonNull(movementId, "Movement ID is required");

        if (!eligibilityChecker.movementExists(movementId)) {
            throw new LoadingPlanNotFoundException(
                    "Movement does not exist: " + movementId
            );
        }

        return repository.findByMovementId(movementId);
    }
}
