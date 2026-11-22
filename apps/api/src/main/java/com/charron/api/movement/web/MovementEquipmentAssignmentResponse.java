
package com.charron.api.movement.web;

import com.charron.api.movement.domain.MovementEquipmentAssignment;
import com.charron.api.movement.domain.MovementEquipmentRole;

import java.time.Instant;
import java.util.UUID;

public record MovementEquipmentAssignmentResponse(
        UUID id,
        UUID movementId,
        UUID equipmentId,
        MovementEquipmentRole role,
        Instant assignedAt
) {

    public static MovementEquipmentAssignmentResponse from(
            MovementEquipmentAssignment assignment
    ) {
        return new MovementEquipmentAssignmentResponse(
                assignment.id(),
                assignment.movementId(),
                assignment.equipmentId(),
                assignment.role(),
                assignment.assignedAt()
        );
    }
}

