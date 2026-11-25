
package com.charron.api.movement.application;

import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MovementTransitionServiceTest {

    private MovementRepository repository;
    private MovementService service;

    @BeforeEach
    void setUp() {
        repository = mock(MovementRepository.class);
        service = new MovementService(repository);
    }

    private Movement plannedMovement() {
        Instant now = Instant.parse("2026-11-20T10:00:00Z");

        return new Movement(
                UUID.randomUUID(),
                "MOV-TEST-001",
                "Denver, CO",
                "Colorado Springs, CO",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-01T12:00:00Z"),
                MovementStatus.PLANNED,
                now,
                now
        );
    }

    @Test
    void dispatchesPlannedMovement() {
        Movement movement = plannedMovement();

        when(repository.findById(movement.id()))
                .thenReturn(Optional.of(movement));

        when(repository.updateStatus(
                eq(movement.id()),
                eq(MovementStatus.PLANNED),
                eq(MovementStatus.DISPATCHED),
                any(Instant.class)
        )).thenReturn(true);

        Movement result = service.transition(
                movement.id(),
                MovementStatus.DISPATCHED
        );

        assertEquals(MovementStatus.DISPATCHED, result.status());
        assertEquals(movement.id(), result.id());

        verify(repository).updateStatus(
                eq(movement.id()),
                eq(MovementStatus.PLANNED),
                eq(MovementStatus.DISPATCHED),
                eq(result.updatedAt())
        );
    }

    @Test
    void startsDispatchedMovement() {
        Movement planned = plannedMovement();

        Movement dispatched = planned.transitionTo(
                MovementStatus.DISPATCHED
        );

        when(repository.findById(dispatched.id()))
                .thenReturn(Optional.of(dispatched));

        when(repository.updateStatus(
                eq(dispatched.id()),
                eq(MovementStatus.DISPATCHED),
                eq(MovementStatus.IN_TRANSIT),
                any(Instant.class)
        )).thenReturn(true);

        Movement result = service.transition(
                dispatched.id(),
                MovementStatus.IN_TRANSIT
        );

        assertEquals(MovementStatus.IN_TRANSIT, result.status());
    }

    @Test
    void completesMovementInTransit() {
        Movement movement = plannedMovement()
                .transitionTo(MovementStatus.DISPATCHED)
                .transitionTo(MovementStatus.IN_TRANSIT);

        when(repository.findById(movement.id()))
                .thenReturn(Optional.of(movement));

        when(repository.updateStatus(
                eq(movement.id()),
                eq(MovementStatus.IN_TRANSIT),
                eq(MovementStatus.COMPLETED),
                any(Instant.class)
        )).thenReturn(true);

        Movement result = service.transition(
                movement.id(),
                MovementStatus.COMPLETED
        );

        assertEquals(MovementStatus.COMPLETED, result.status());
    }

    @Test
    void cancelsPlannedMovement() {
        Movement movement = plannedMovement();

        when(repository.findById(movement.id()))
                .thenReturn(Optional.of(movement));

        when(repository.updateStatus(
                eq(movement.id()),
                eq(MovementStatus.PLANNED),
                eq(MovementStatus.CANCELLED),
                any(Instant.class)
        )).thenReturn(true);

        Movement result = service.transition(
                movement.id(),
                MovementStatus.CANCELLED
        );

        assertEquals(MovementStatus.CANCELLED, result.status());
    }

    @Test
    void rejectsInvalidTransitionWithoutUpdatingDatabase() {
        Movement movement = plannedMovement();

        when(repository.findById(movement.id()))
                .thenReturn(Optional.of(movement));

        assertThrows(
                IllegalStateException.class,
                () -> service.transition(
                        movement.id(),
                        MovementStatus.COMPLETED
                )
        );

        verify(repository, never()).updateStatus(
                any(), any(), any(), any()
        );
    }

    @Test
    void rejectsMissingMovement() {
        UUID missingId = UUID.randomUUID();

        when(repository.findById(missingId))
                .thenReturn(Optional.empty());

        assertThrows(
                MovementNotFoundException.class,
                () -> service.transition(
                        missingId,
                        MovementStatus.DISPATCHED
                )
        );

        verify(repository, never()).updateStatus(
                any(), any(), any(), any()
        );
    }

    @Test
    void detectsConcurrentStatusChange() {
        Movement movement = plannedMovement();

        when(repository.findById(movement.id()))
                .thenReturn(Optional.of(movement));

        when(repository.updateStatus(
                eq(movement.id()),
                eq(MovementStatus.PLANNED),
                eq(MovementStatus.DISPATCHED),
                any(Instant.class)
        )).thenReturn(false);

        assertThrows(
                MovementTransitionConflictException.class,
                () -> service.transition(
                        movement.id(),
                        MovementStatus.DISPATCHED
                )
        );
    }

    @Test
    void rejectsNullTargetStatus() {
        Movement movement = plannedMovement();

        assertThrows(
                NullPointerException.class,
                () -> service.transition(movement.id(), null)
        );

        verifyNoInteractions(repository);
    }
}
