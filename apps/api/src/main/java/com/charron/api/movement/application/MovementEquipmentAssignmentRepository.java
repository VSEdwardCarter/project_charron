
package com.charron.api.movement.application;

import com.charron.api.movement.domain.MovementEquipmentAssignment;
import com.charron.api.movement.domain.MovementEquipmentRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovementEquipmentAssignmentRepository {

    void insert(MovementEquipmentAssignment assignment);

    Optional<MovementEquipmentAssignment> findById(UUID id);

    List<MovementEquipmentAssignment> findByMovementId(
            UUID movementId
    );

    boolean existsByMovementAndEquipmentAndRole(
            UUID movementId,
            UUID equipmentId,
            MovementEquipmentRole role
    );
}
