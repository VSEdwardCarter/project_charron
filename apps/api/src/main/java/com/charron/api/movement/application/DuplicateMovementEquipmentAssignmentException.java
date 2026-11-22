
package com.charron.api.movement.application;

import com.charron.api.movement.domain.MovementEquipmentRole;

import java.util.UUID;

public class DuplicateMovementEquipmentAssignmentException
        extends RuntimeException {

    public DuplicateMovementEquipmentAssignmentException(
            UUID movementId,
            UUID equipmentId,
            MovementEquipmentRole role
    ) {
        super(
                "Equipment " + equipmentId
                        + " is already assigned to movement "
                        + movementId
                        + " in role " + role
        );
    }
}
