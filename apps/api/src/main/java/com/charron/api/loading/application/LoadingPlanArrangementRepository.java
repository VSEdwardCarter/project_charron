
package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlanArrangement;

import java.util.Optional;
import java.util.UUID;

public interface LoadingPlanArrangementRepository {

    void save(LoadingPlanArrangement arrangement);

    Optional<LoadingPlanArrangement> findByLoadingPlanId(
            UUID loadingPlanId
    );

    void deleteByLoadingPlanId(UUID loadingPlanId);
}
