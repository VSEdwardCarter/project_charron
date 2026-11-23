
package com.charron.api.loading.application;

import java.util.UUID;

public interface LoadingPlanEligibilityChecker {

    boolean movementExists(UUID movementId);

    boolean equipmentAssignedToMovement(
            UUID movementId,
            UUID equipmentId
    );
}
