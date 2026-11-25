
package com.charron.api.movement.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MovementStatusTest {

    @Test
    void plannedMovementCanBeDispatchedOrCancelled() {
        assertEquals(
                Set.of(
                        MovementStatus.DISPATCHED,
                        MovementStatus.CANCELLED
                ),
                MovementStatus.PLANNED.allowedTransitions()
        );
    }

    @Test
    void dispatchedMovementCanEnterTransitOrBeCancelled() {
        assertEquals(
                Set.of(
                        MovementStatus.IN_TRANSIT,
                        MovementStatus.CANCELLED
                ),
                MovementStatus.DISPATCHED.allowedTransitions()
        );
    }

    @Test
    void inTransitMovementCanOnlyBeCompleted() {
        assertEquals(
                Set.of(MovementStatus.COMPLETED),
                MovementStatus.IN_TRANSIT.allowedTransitions()
        );
    }

    @Test
    void completedMovementCannotTransition() {
        assertTrue(
                MovementStatus.COMPLETED.allowedTransitions().isEmpty()
        );
    }

    @Test
    void cancelledMovementCannotTransition() {
        assertTrue(
                MovementStatus.CANCELLED.allowedTransitions().isEmpty()
        );
    }

    @Test
    void cannotTransitionToSameStatus() {
        for (MovementStatus status : MovementStatus.values()) {
            assertFalse(status.canTransitionTo(status));
        }
    }

    @Test
    void cannotTransitionToNull() {
        for (MovementStatus status : MovementStatus.values()) {
            assertFalse(status.canTransitionTo(null));
        }
    }

    @Test
    void completedAndCancelledAreTerminal() {
        assertTrue(MovementStatus.COMPLETED.isTerminal());
        assertTrue(MovementStatus.CANCELLED.isTerminal());

        assertFalse(MovementStatus.PLANNED.isTerminal());
        assertFalse(MovementStatus.DISPATCHED.isTerminal());
        assertFalse(MovementStatus.IN_TRANSIT.isTerminal());
    }
}
