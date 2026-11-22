
package com.charron.api.movement.application;

import com.charron.api.movement.domain.MovementEquipmentAssignment;
import com.charron.api.movement.domain.MovementEquipmentRole;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class MovementEquipmentAssignmentService {

    private final MovementEquipmentAssignmentRepository assignmentRepository;
    private final MovementRepository movementRepository;
    private final EquipmentExistenceChecker equipmentExistenceChecker;

    public MovementEquipmentAssignmentService(
            MovementEquipmentAssignmentRepository assignmentRepository,
            MovementRepository movementRepository,
            EquipmentExistenceChecker equipmentExistenceChecker
    ) {
        this.assignmentRepository =
                Objects.requireNonNull(assignmentRepository);

        this.movementRepository =
                Objects.requireNonNull(movementRepository);

        this.equipmentExistenceChecker =
                Objects.requireNonNull(equipmentExistenceChecker);
    }

    @Transactional
    public MovementEquipmentAssignment assign(
            UUID movementId,
            UUID equipmentId,
            MovementEquipmentRole role
    ) {
        Objects.requireNonNull(movementId, "Movement ID is required");
        Objects.requireNonNull(equipmentId, "Equipment ID is required");
        Objects.requireNonNull(role, "Assignment role is required");

        if (movementRepository.findById(movementId).isEmpty()) {
            throw new MovementNotFoundException(movementId);
        }

        if (!equipmentExistenceChecker.existsById(equipmentId)) {
            throw new EquipmentNotFoundException(equipmentId);
        }

        if (assignmentRepository.existsByMovementAndEquipmentAndRole(
                movementId,
                equipmentId,
                role
        )) {
            throw new DuplicateMovementEquipmentAssignmentException(
                    movementId,
                    equipmentId,
                    role
            );
        }

        MovementEquipmentAssignment assignment =
                MovementEquipmentAssignment.assign(
                        movementId,
                        equipmentId,
                        role
                );


        try {
            assignmentRepository.insert(assignment);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateMovementEquipmentAssignmentException(
                    movementId,
                    equipmentId,
                    role
            );
        }


        return assignment;
    }

    @Transactional(readOnly = true)
    public List<MovementEquipmentAssignment> findByMovementId(
            UUID movementId
    ) {
        Objects.requireNonNull(movementId, "Movement ID is required");

        if (movementRepository.findById(movementId).isEmpty()) {
            throw new MovementNotFoundException(movementId);
        }

        return assignmentRepository.findByMovementId(movementId);
    }
}
