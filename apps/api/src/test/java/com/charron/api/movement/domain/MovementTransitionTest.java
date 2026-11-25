
package com.charron.api.movement.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MovementTransitionTest {

    private Movement plannedMovement() {
        return new Movement(
                UUID.randomUUID(),
                "MOV-TEST-001",
                "Denver, CO",
                "Colorado Springs, CO",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-01T12:00:00Z"),
                MovementStatus.PLANNED,
                Instant.parse("2026-11-20T10:00:00Z"),
                Instant.parse("2026-11-20T10:00:00Z")
        );
    }

    @Test
    void plannedMovementCanBeDispatched() {
        Movement original = plannedMovement();

        Movement updated = original.transitionTo(
                MovementStatus.DISPATCHED
        );

        assertEquals(MovementStatus.DISPATCHED, updated.status());
        assertEquals(MovementStatus.PLANNED, original.status());
        assertEquals(original.id(), updated.id());
        assertEquals(original.reference(), updated.reference());
        assertEquals(original.origin(), updated.origin());
        assertEquals(original.destination(), updated.destination());
        assertEquals(original.plannedDeparture(), updated.plannedDeparture());
        assertEquals(original.plannedArrival(), updated.plannedArrival());
        assertEquals(original.createdAt(), updated.createdAt());
        assertFalse(updated.updatedAt().isBefore(original.updatedAt()));
    }

    @Test
    void movementCanProgressThroughCompleteLifecycle() {
        Movement movement = plannedMovement()
                .transitionTo(MovementStatus.DISPATCHED)
                .transitionTo(MovementStatus.IN_TRANSIT)
                .transitionTo(MovementStatus.COMPLETED);

        assertEquals(MovementStatus.COMPLETED, movement.status());
    }

    @Test
    void plannedMovementCanBeCancelled() {
        Movement movement = plannedMovement()
                .transitionTo(MovementStatus.CANCELLED);

        assertEquals(MovementStatus.CANCELLED, movement.status());
    }

    @Test
    void cannotSkipDirectlyToCompleted() {
        Movement movement = plannedMovement();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> movement.transitionTo(MovementStatus.COMPLETED)
        );

        assertTrue(exception.getMessage().contains(
                "PLANNED -> COMPLETED"
        ));
    }

    @Test
    void cannotRepeatSameStatus() {
        Movement movement = plannedMovement();

        assertThrows(
                IllegalStateException.class,
                () -> movement.transitionTo(MovementStatus.PLANNED)
        );
    }

    @Test
    void cannotChangeCompletedMovement() {
        Movement movement = plannedMovement()
                .transitionTo(MovementStatus.DISPATCHED)
                .transitionTo(MovementStatus.IN_TRANSIT)
                .transitionTo(MovementStatus.COMPLETED);

        assertThrows(
                IllegalStateException.class,
                () -> movement.transitionTo(MovementStatus.CANCELLED)
        );
    }

    @Test
    void cannotChangeCancelledMovement() {
        Movement movement = plannedMovement()
                .transitionTo(MovementStatus.CANCELLED);

        assertThrows(
                IllegalStateException.class,
                () -> movement.transitionTo(MovementStatus.DISPATCHED)
        );
    }

    @Test
    void cannotTransitionToNull() {
        Movement movement = plannedMovement();

        assertThrows(
                NullPointerException.class,
                () -> movement.transitionTo(null)
        );
    }

    @Test
    void timestampNeverMovesBackward() {
        Movement original = new Movement(
                UUID.randomUUID(),
                "MOV-FUTURE",
                "Denver",
                "Pueblo",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-01T12:00:00Z"),
                MovementStatus.PLANNED,
                Instant.parse("2026-11-20T10:00:00Z"),
                Instant.parse("2099-01-01T00:00:00Z")
        );

        Movement updated = original.transitionTo(
                MovementStatus.DISPATCHED
        );

        assertEquals(original.updatedAt(), updated.updatedAt());
    }
}
