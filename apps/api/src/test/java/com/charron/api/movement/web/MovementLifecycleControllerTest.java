
package com.charron.api.movement.web;

import com.charron.api.movement.application.MovementNotFoundException;
import com.charron.api.movement.application.MovementService;
import com.charron.api.movement.application.MovementTransitionConflictException;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovementLifecycleControllerTest {

    private MovementService service;
    private MovementController controller;
    private MovementExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        service = mock(MovementService.class);
        controller = new MovementController(service);
        exceptionHandler = new MovementExceptionHandler();
    }

    private Movement plannedMovement() {
        return new Movement(
                UUID.randomUUID(),
                "MOV-LIFECYCLE-001",
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z"),
                MovementStatus.PLANNED,
                Instant.parse("2026-11-20T10:00:00Z"),
                Instant.parse("2026-11-20T10:00:00Z")
        );
    }

    @Test
    void dispatchesPlannedMovement() {
        Movement movement = plannedMovement();

        Movement dispatched = movement.transitionTo(
                MovementStatus.DISPATCHED
        );

        when(service.transition(
                movement.id(),
                MovementStatus.DISPATCHED
        )).thenReturn(dispatched);

        MovementResponse response =
                controller.dispatch(movement.id());

        assertEquals(MovementStatus.DISPATCHED, response.status());
        assertEquals(movement.id(), response.id());
        assertEquals(movement.reference(), response.reference());

        verify(service).transition(
                movement.id(),
                MovementStatus.DISPATCHED
        );
    }

    @Test
    void startsDispatchedMovement() {
        Movement movement = plannedMovement()
                .transitionTo(MovementStatus.DISPATCHED);

        Movement started = movement.transitionTo(
                MovementStatus.IN_TRANSIT
        );

        when(service.transition(
                movement.id(),
                MovementStatus.IN_TRANSIT
        )).thenReturn(started);

        MovementResponse response =
                controller.start(movement.id());

        assertEquals(MovementStatus.IN_TRANSIT, response.status());

        verify(service).transition(
                movement.id(),
                MovementStatus.IN_TRANSIT
        );
    }

    @Test
    void completesMovementInTransit() {
        Movement movement = plannedMovement()
                .transitionTo(MovementStatus.DISPATCHED)
                .transitionTo(MovementStatus.IN_TRANSIT);

        Movement completed = movement.transitionTo(
                MovementStatus.COMPLETED
        );

        when(service.transition(
                movement.id(),
                MovementStatus.COMPLETED
        )).thenReturn(completed);

        MovementResponse response =
                controller.complete(movement.id());

        assertEquals(MovementStatus.COMPLETED, response.status());

        verify(service).transition(
                movement.id(),
                MovementStatus.COMPLETED
        );
    }

    @Test
    void cancelsPlannedMovement() {
        Movement movement = plannedMovement();

        Movement cancelled = movement.transitionTo(
                MovementStatus.CANCELLED
        );

        when(service.transition(
                movement.id(),
                MovementStatus.CANCELLED
        )).thenReturn(cancelled);

        MovementResponse response =
                controller.cancel(movement.id());

        assertEquals(MovementStatus.CANCELLED, response.status());

        verify(service).transition(
                movement.id(),
                MovementStatus.CANCELLED
        );
    }

    @Test
    void mapsMissingMovementTo404() {
        UUID missingId = UUID.randomUUID();

        MovementNotFoundException exception =
                new MovementNotFoundException(missingId);

        ProblemDetail problem =
                exceptionHandler.handleNotFound(exception);

        assertEquals(404, problem.getStatus());
        assertEquals("Movement Not Found", problem.getTitle());
    }

    @Test
    void mapsInvalidTransitionTo409() {
        IllegalStateException exception =
                new IllegalStateException(
                        "Invalid movement transition: PLANNED -> COMPLETED"
                );

        ProblemDetail problem =
                exceptionHandler.handleInvalidTransition(exception);

        assertEquals(409, problem.getStatus());
        assertEquals(
                "Invalid Movement Transition",
                problem.getTitle()
        );
        assertTrue(
                problem.getDetail().contains("PLANNED -> COMPLETED")
        );
    }

    @Test
    void mapsConcurrentUpdateTo409() {
        UUID movementId = UUID.randomUUID();

        MovementTransitionConflictException exception =
                new MovementTransitionConflictException(
                        movementId,
                        MovementStatus.PLANNED,
                        MovementStatus.DISPATCHED
                );

        ProblemDetail problem =
                exceptionHandler.handleTransitionConflict(exception);

        assertEquals(409, problem.getStatus());
        assertEquals(
                "Movement Transition Conflict",
                problem.getTitle()
        );
    }

    @Test
    void controllerPropagatesServiceConflict() {
        Movement movement = plannedMovement();

        when(service.transition(
                movement.id(),
                MovementStatus.DISPATCHED
        )).thenThrow(
                new MovementTransitionConflictException(
                        movement.id(),
                        MovementStatus.PLANNED,
                        MovementStatus.DISPATCHED
                )
        );

        assertThrows(
                MovementTransitionConflictException.class,
                () -> controller.dispatch(movement.id())
        );
    }
}
