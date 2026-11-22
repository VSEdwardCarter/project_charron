
package com.charron.api.movement.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MovementEquipmentAssignmentTest {

    @Test
    void createsEquipmentAssignment() {
        UUID movementId = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();

        MovementEquipmentAssignment assignment =
                MovementEquipmentAssignment.assign(
                        movementId,
                        equipmentId,
                        MovementEquipmentRole.POWER_UNIT
                );

        assertNotNull(assignment.id());
        assertEquals(movementId, assignment.movementId());
        assertEquals(equipmentId, assignment.equipmentId());
        assertEquals(
                MovementEquipmentRole.POWER_UNIT,
                assignment.role()
        );
        assertNotNull(assignment.assignedAt());
    }

    @Test
    void rejectsMissingMovementId() {
        assertThrows(
                NullPointerException.class,
                () -> MovementEquipmentAssignment.assign(
                        null,
                        UUID.randomUUID(),
                        MovementEquipmentRole.CARGO_CARRIER
                )
        );
    }

    @Test
    void rejectsMissingEquipmentId() {
        assertThrows(
                NullPointerException.class,
                () -> MovementEquipmentAssignment.assign(
                        UUID.randomUUID(),
                        null,
                        MovementEquipmentRole.CARGO_CARRIER
                )
        );
    }

    @Test
    void rejectsMissingRole() {
        assertThrows(
                NullPointerException.class,
                () -> MovementEquipmentAssignment.assign(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null
                )
        );
    }
}
