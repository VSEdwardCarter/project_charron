
package com.charron.api.movement.web;

import com.charron.api.movement.domain.MovementEquipmentRole;

import java.util.UUID;

public record MovementEquipmentAssignmentRequest(
        UUID equipmentId,
        MovementEquipmentRole role
) {
}
