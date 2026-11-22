
package com.charron.api.movement.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MovementTest {

    private static final Instant DEPARTURE =
            Instant.parse("2026-12-01T08:00:00Z");

    private static final Instant ARRIVAL =
            Instant.parse("2026-12-02T08:00:00Z");

    @Test
    void newMovementStartsPlanned() {
        Movement movement = Movement.plan(
                "MOV-001",
                "Denver, CO",
                "Dallas, TX",
                DEPARTURE,
                ARRIVAL
        );

        assertNotNull(movement.id());
        assertEquals("MOV-001", movement.reference());
        assertEquals("Denver, CO", movement.origin());
        assertEquals("Dallas, TX", movement.destination());
        assertEquals(MovementStatus.PLANNED, movement.status());
        assertEquals(movement.createdAt(), movement.updatedAt());
    }

    @Test
    void arrivalMustFollowDeparture() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Movement.plan(
                        "MOV-002",
                        "Denver, CO",
                        "Dallas, TX",
                        DEPARTURE,
                        DEPARTURE
                )
        );
    }

    @Test
    void movementReferenceCannotBeBlank() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Movement.plan(
                        "   ",
                        "Denver, CO",
                        "Dallas, TX",
                        DEPARTURE,
                        ARRIVAL
                )
        );
    }

    @Test
    void movementRequiresOriginAndDestination() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Movement.plan(
                        "MOV-003",
                        "",
                        "Dallas, TX",
                        DEPARTURE,
                        ARRIVAL
                )
        );
    }

    @Test
    void movementTrimsInputValues() {
        Movement movement = Movement.plan(
                " MOV-004 ",
                " Denver, CO ",
                " Dallas, TX ",
                DEPARTURE,
                ARRIVAL
        );

        assertEquals("MOV-004", movement.reference());
        assertEquals("Denver, CO", movement.origin());
        assertEquals("Dallas, TX", movement.destination());
    }
}
