
package com.charron.api.loading.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoadingPlanTest {

    @Test
    void createsLoadingPlan() {
        UUID movementId = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();
        UUID cargoSpaceId = UUID.randomUUID();

        LoadingPlan plan = LoadingPlan.create(
                movementId,
                equipmentId,
                cargoSpaceId
        );

        assertNotNull(plan.id());
        assertEquals(movementId, plan.movementId());
        assertEquals(equipmentId, plan.equipmentId());
        assertEquals(cargoSpaceId, plan.cargoSpaceId());
        assertNotNull(plan.createdAt());
        assertEquals(plan.createdAt(), plan.updatedAt());
    }

    @Test
    void rejectsMissingMovement() {
        assertThrows(
                NullPointerException.class,
                () -> LoadingPlan.create(
                        null,
                        UUID.randomUUID(),
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void rejectsMissingEquipment() {
        assertThrows(
                NullPointerException.class,
                () -> LoadingPlan.create(
                        UUID.randomUUID(),
                        null,
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void rejectsMissingCargoSpace() {
        assertThrows(
                NullPointerException.class,
                () -> LoadingPlan.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null
                )
        );
    }

    @Test
    void rejectsInvalidTimestamps() {
        Instant created = Instant.parse("2026-12-01T12:00:00Z");
        Instant updated = Instant.parse("2026-12-01T11:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> new LoadingPlan(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        created,
                        updated
                )
        );
    }
}
