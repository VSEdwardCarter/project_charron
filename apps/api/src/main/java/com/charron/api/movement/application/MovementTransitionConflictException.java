
package com.charron.api.movement.application;

import com.charron.api.movement.domain.MovementStatus;

import java.util.UUID;

public class MovementTransitionConflictException
        extends RuntimeException {

    public MovementTransitionConflictException(
            UUID movementId,
            MovementStatus expectedStatus,
            MovementStatus targetStatus
    ) {
        super(
                "Movement " + movementId
                        + " could not transition from "
                        + expectedStatus
                        + " to "
                        + targetStatus
                        + " because its database state changed"
        );
    }
}
