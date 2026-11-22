
package com.charron.api.movement.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MovementEquipmentAssignment(
        UUID id,
        UUID movementId,
        UUID equipmentId,
        MovementEquipmentRole role,
        Instant assignedAt
) {
    public MovementEquipmentAssignment {
        Objects.requireNonNull(id, "Assignment ID is required");
        Objects.requireNonNull(movementId, "Movement ID is required");
        Objects.requireNonNull(equipmentId, "Equipment ID is required");
        Objects.requireNonNull(role, "Equipment role is required");
        Objects.requireNonNull(assignedAt, "Assignment timestamp is required");
    }

    public static MovementEquipmentAssignment assign(
            UUID movementId,
            UUID equipmentId,
            MovementEquipmentRole role
    ) {
        return new MovementEquipmentAssignment(
                UUID.randomUUID(),
                movementId,
                equipmentId,
                role,
                Instant.now()
        );
    }
}
