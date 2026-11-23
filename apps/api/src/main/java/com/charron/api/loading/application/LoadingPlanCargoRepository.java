
package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlanCargo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadingPlanCargoRepository {

    void insert(LoadingPlanCargo cargoLine);

    Optional<LoadingPlanCargo> findById(UUID id);

    List<LoadingPlanCargo> findByLoadingPlanId(UUID loadingPlanId);

    boolean existsByLoadingPlanAndCargo(
            UUID loadingPlanId,
            UUID cargoId
    );

    void updateQuantity(LoadingPlanCargo cargoLine);

    void deleteById(UUID id);
}
