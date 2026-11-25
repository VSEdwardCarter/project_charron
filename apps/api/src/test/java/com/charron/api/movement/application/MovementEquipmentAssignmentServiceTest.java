
package com.charron.api.movement.application;

import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementEquipmentAssignment;
import com.charron.api.movement.domain.MovementEquipmentRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MovementEquipmentAssignmentServiceTest {

    private MovementEquipmentAssignmentRepository assignmentRepository;
    private MovementRepository movementRepository;
    private MovementEquipmentAssignmentService service;
    private EquipmentExistenceChecker equipmentExistenceChecker;

    private UUID movementId;
    private UUID equipmentId;
    private Movement movement;


    @BeforeEach
    void setUp() {
        assignmentRepository =
                mock(MovementEquipmentAssignmentRepository.class);

        movementRepository =
                mock(MovementRepository.class);

        equipmentExistenceChecker =
                mock(EquipmentExistenceChecker.class);

        service = new MovementEquipmentAssignmentService(
                assignmentRepository,
                movementRepository,
                equipmentExistenceChecker
        );

        movementId = UUID.randomUUID();
        equipmentId = UUID.randomUUID();

        movement = Movement.plan(
                "MOV-TEST",
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T15:00:00Z"),
                Instant.parse("2026-12-02T15:00:00Z")
        );

        when(equipmentExistenceChecker.existsById(equipmentId))
                .thenReturn(true);
    }


    @Test
    void assignsEquipmentToExistingMovement() {
        when(movementRepository.findByIdForUpdate(movementId))
                .thenReturn(Optional.of(movement));

        MovementEquipmentAssignment assignment =
                service.assign(
                        movementId,
                        equipmentId,
                        MovementEquipmentRole.POWER_UNIT
                );

        assertEquals(movementId, assignment.movementId());
        assertEquals(equipmentId, assignment.equipmentId());
        assertEquals(
                MovementEquipmentRole.POWER_UNIT,
                assignment.role()
        );
        assertNotNull(assignment.id());
        assertNotNull(assignment.assignedAt());

        verify(assignmentRepository).insert(assignment);
    }

    @Test
    void rejectsAssignmentForUnknownMovement() {
        when(movementRepository.findByIdForUpdate(movementId))
                .thenReturn(Optional.empty());

        assertThrows(
                MovementNotFoundException.class,
                () -> service.assign(
                        movementId,
                        equipmentId,
                        MovementEquipmentRole.POWER_UNIT
                )
        );

        verify(assignmentRepository, never())
                .insert(any());
    }

    @Test
    void rejectsDuplicateAssignment() {
        when(movementRepository.findByIdForUpdate(movementId))
                .thenReturn(Optional.of(movement));

        when(assignmentRepository
                .existsByMovementAndEquipmentAndRole(
                        movementId,
                        equipmentId,
                        MovementEquipmentRole.POWER_UNIT
                ))
                .thenReturn(true);

        assertThrows(
                DuplicateMovementEquipmentAssignmentException.class,
                () -> service.assign(
                        movementId,
                        equipmentId,
                        MovementEquipmentRole.POWER_UNIT
                )
        );

        verify(assignmentRepository, never())
                .insert(any());
    }

    @Test
    void retrievesAssignmentsForMovement() {
        when(movementRepository.findById(movementId))
                .thenReturn(Optional.of(movement));

        MovementEquipmentAssignment assignment =
                MovementEquipmentAssignment.assign(
                        movementId,
                        equipmentId,
                        MovementEquipmentRole.CARGO_CARRIER
                );

        when(assignmentRepository.findByMovementId(movementId))
                .thenReturn(List.of(assignment));

        List<MovementEquipmentAssignment> results =
                service.findByMovementId(movementId);

        assertEquals(1, results.size());
        assertEquals(assignment, results.getFirst());
    }

    @Test
    void rejectsRetrievalForUnknownMovement() {
        when(movementRepository.findById(movementId))
                .thenReturn(Optional.empty());

        assertThrows(
                MovementNotFoundException.class,
                () -> service.findByMovementId(movementId)
        );

        verify(assignmentRepository, never())
                .findByMovementId(any());
    }


    @Test
    void rejectsAssignmentForUnknownEquipment() {
        when(movementRepository.findByIdForUpdate(movementId))
                .thenReturn(Optional.of(movement));

        UUID unknownEquipmentId = UUID.randomUUID();

        when(equipmentExistenceChecker.existsById(unknownEquipmentId))
                .thenReturn(false);

        assertThrows(
                EquipmentNotFoundException.class,
                () -> service.assign(
                        movementId,
                        unknownEquipmentId,
                        MovementEquipmentRole.POWER_UNIT
                )
        );

        verify(assignmentRepository, never())
                .insert(any());
    }

    @Test
    void translatesDatabaseDuplicateConflict() {
        when(movementRepository.findByIdForUpdate(movementId))
                .thenReturn(Optional.of(movement));

        doThrow(new DuplicateKeyException(
                "Duplicate movement equipment assignment"
        )).when(assignmentRepository).insert(any());

        assertThrows(
                DuplicateMovementEquipmentAssignmentException.class,
                () -> service.assign(
                        movementId,
                        equipmentId,
                        MovementEquipmentRole.POWER_UNIT
                )
        );
    }


}
