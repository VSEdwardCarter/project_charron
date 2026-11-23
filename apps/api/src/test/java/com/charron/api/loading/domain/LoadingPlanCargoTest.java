
package com.charron.api.loading.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoadingPlanCargoTest {

    @Test
    void createsCargoLine() {
        UUID planId = UUID.randomUUID();
        UUID cargoId = UUID.randomUUID();

        LoadingPlanCargo line = LoadingPlanCargo.create(
                planId,
                cargoId,
                3
        );

        assertNotNull(line.id());
        assertEquals(planId, line.loadingPlanId());
        assertEquals(cargoId, line.cargoId());
        assertEquals(3, line.quantity());
        assertEquals(line.createdAt(), line.updatedAt());
    }

    @Test
    void rejectsZeroQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LoadingPlanCargo.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        0
                )
        );
    }

    @Test
    void rejectsNegativeQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LoadingPlanCargo.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        -2
                )
        );
    }

    @Test
    void updatesQuantityWithoutChangingIdentity() {
        LoadingPlanCargo original = LoadingPlanCargo.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                2
        );

        LoadingPlanCargo updated = original.withQuantity(5);

        assertEquals(original.id(), updated.id());
        assertEquals(original.loadingPlanId(), updated.loadingPlanId());
        assertEquals(original.cargoId(), updated.cargoId());
        assertEquals(original.createdAt(), updated.createdAt());
        assertEquals(5, updated.quantity());
        assertFalse(updated.updatedAt().isBefore(original.updatedAt()));
    }

    @Test
    void rejectsInvalidTimestamps() {
        Instant created = Instant.parse("2026-12-01T12:00:00Z");
        Instant updated = Instant.parse("2026-12-01T11:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> new LoadingPlanCargo(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        1,
                        created,
                        updated
                )
        );
    }
}
