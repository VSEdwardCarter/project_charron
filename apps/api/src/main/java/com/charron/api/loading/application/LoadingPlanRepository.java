
package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadingPlanRepository {

    void insert(LoadingPlan plan);

    Optional<LoadingPlan> findById(UUID id);

    List<LoadingPlan> findByMovementId(UUID movementId);

    boolean existsByMovementAndCargoSpace(
            UUID movementId,
            UUID cargoSpaceId
    );
}
