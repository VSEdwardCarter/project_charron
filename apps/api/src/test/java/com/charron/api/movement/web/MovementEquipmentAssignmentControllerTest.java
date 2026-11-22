
package com.charron.api.movement.web;

import com.charron.api.movement.application.DuplicateMovementEquipmentAssignmentException;
import com.charron.api.movement.application.EquipmentNotFoundException;
import com.charron.api.movement.application.MovementEquipmentAssignmentService;
import com.charron.api.movement.application.MovementNotFoundException;
import com.charron.api.movement.domain.MovementEquipmentAssignment;
import com.charron.api.movement.domain.MovementEquipmentRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovementEquipmentAssignmentControllerTest {

    private MovementEquipmentAssignmentService service;
    private MovementEquipmentAssignmentController controller;
    private MovementEquipmentAssignmentExceptionHandler handler;

    private UUID movementId;
    private UUID equipmentId;
    private MovementEquipmentAssignment assignment;

    @BeforeEach
    void setUp() {
        service = mock(MovementEquipmentAssignmentService.class);
        controller = new MovementEquipmentAssignmentController(service);
        handler = new MovementEquipmentAssignmentExceptionHandler();

        movementId = UUID.randomUUID();
        equipmentId = UUID.randomUUID();

        assignment = new MovementEquipmentAssignment(
                UUID.randomUUID(),
                movementId,
                equipmentId,
                MovementEquipmentRole.POWER_UNIT,
                Instant.parse("2026-12-01T15:00:00Z")
        );
    }

    @Test
    void createsEquipmentAssignment() {
        when(service.assign(
                movementId,
                equipmentId,
                MovementEquipmentRole.POWER_UNIT
        )).thenReturn(assignment);

        MovementEquipmentAssignmentRequest request =
                new MovementEquipmentAssignmentRequest(
                        equipmentId,
                        MovementEquipmentRole.POWER_UNIT
                );

        ResponseEntity<MovementEquipmentAssignmentResponse> response =
                controller.assign(movementId, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        assertEquals(
                "/api/movements/" + movementId
                        + "/equipment/" + assignment.id(),
                response.getHeaders().getLocation().toString()
        );

        assertNotNull(response.getBody());
        assertEquals(assignment.id(), response.getBody().id());
        assertEquals(equipmentId, response.getBody().equipmentId());
        assertEquals(
                MovementEquipmentRole.POWER_UNIT,
                response.getBody().role()
        );
    }

    @Test
    void retrievesEquipmentAssignments() {
        when(service.findByMovementId(movementId))
                .thenReturn(List.of(assignment));

        ResponseEntity<List<MovementEquipmentAssignmentResponse>> response =
                controller.findAll(movementId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(
                assignment.id(),
                response.getBody().getFirst().id()
        );
    }

    @Test
    void rejectsMissingEquipmentId() {
        MovementEquipmentAssignmentRequest request =
                new MovementEquipmentAssignmentRequest(
                        null,
                        MovementEquipmentRole.POWER_UNIT
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.assign(movementId, request)
        );

        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingRole() {
        MovementEquipmentAssignmentRequest request =
                new MovementEquipmentAssignmentRequest(
                        equipmentId,
                        null
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.assign(movementId, request)
        );

        verifyNoInteractions(service);
    }

    @Test
    void handlesUnknownMovement() {
        ProblemDetail problem = handler.handleMovementNotFound(
                new MovementNotFoundException(movementId)
        );

        assertEquals(404, problem.getStatus());
        assertEquals("Movement Not Found", problem.getTitle());
    }

    @Test
    void handlesDuplicateAssignment() {
        ProblemDetail problem = handler.handleDuplicateAssignment(
                new DuplicateMovementEquipmentAssignmentException(
                        movementId,
                        equipmentId,
                        MovementEquipmentRole.POWER_UNIT
                )
        );

        assertEquals(409, problem.getStatus());
        assertEquals(
                "Duplicate Equipment Assignment",
                problem.getTitle()
        );
    }

    @Test
    void handlesInvalidAssignment() {
        ProblemDetail problem = handler.handleInvalidAssignment(
                new IllegalArgumentException(
                        "Equipment ID and assignment role are required"
                )
        );

        assertEquals(400, problem.getStatus());
        assertEquals(
                "Invalid Equipment Assignment",
                problem.getTitle()
        );
    }


    @Test
    void handlesUnknownEquipment() {
        ProblemDetail problem = handler.handleEquipmentNotFound(
                new EquipmentNotFoundException(equipmentId)
        );

        assertEquals(404, problem.getStatus());
        assertEquals("Equipment Not Found", problem.getTitle());
        assertTrue(problem.getDetail().contains(equipmentId.toString()));
    }

}
