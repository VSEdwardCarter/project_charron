
package com.charron.api.fleet.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentModificationLifecycleTest {

    private static final Instant INSTALLED =
            Instant.parse("2026-11-01T10:00:00Z");

    private static final Instant REMOVED =
            Instant.parse("2026-11-15T10:00:00Z");

    private EquipmentModificationEvent event(
            UUID modificationId,
            EquipmentModificationEventType type,
            Instant effectiveAt
    ) {
        return new EquipmentModificationEvent(
                UUID.randomUUID(),
                modificationId,
                type,
                effectiveAt,
                effectiveAt.plusSeconds(3600)
        );
    }

    @Test
    void reconstructsActiveModification() {
        UUID id = UUID.randomUUID();

        EquipmentModificationLifecycle lifecycle =
                EquipmentModificationLifecycle.reconstruct(
                        id,
                        List.of(event(
                                id,
                                EquipmentModificationEventType.RECORDED,
                                INSTALLED
                        ))
                );

        assertTrue(lifecycle.isEffectiveAt(INSTALLED));
        assertNull(lifecycle.removedAt());
    }

    @Test
    void reconstructsRemovedModification() {
        UUID id = UUID.randomUUID();

        EquipmentModificationLifecycle lifecycle =
                EquipmentModificationLifecycle.reconstruct(
                        id,
                        List.of(
                                event(id,
                                        EquipmentModificationEventType.RECORDED,
                                        INSTALLED),
                                event(id,
                                        EquipmentModificationEventType.REMOVED,
                                        REMOVED)
                        )
                );

        assertTrue(lifecycle.isEffectiveAt(INSTALLED));
        assertFalse(lifecycle.isEffectiveAt(REMOVED));
        assertEquals(REMOVED, lifecycle.removedAt());
    }

    @Test
    void rejectsDuplicateInitialEvents() {
        UUID id = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentModificationLifecycle.reconstruct(
                        id,
                        List.of(
                                event(id,
                                        EquipmentModificationEventType.RECORDED,
                                        INSTALLED),
                                event(id,
                                        EquipmentModificationEventType.RECORDED,
                                        INSTALLED)
                        )
                )
        );
    }

    @Test
    void rejectsDuplicateRemovalEvents() {
        UUID id = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentModificationLifecycle.reconstruct(
                        id,
                        List.of(
                                event(id,
                                        EquipmentModificationEventType.RECORDED,
                                        INSTALLED),
                                event(id,
                                        EquipmentModificationEventType.REMOVED,
                                        REMOVED),
                                event(id,
                                        EquipmentModificationEventType.REMOVED,
                                        REMOVED.plusSeconds(3600))
                        )
                )
        );
    }

    @Test
    void rejectsEventsFromDifferentModification() {
        UUID id = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentModificationLifecycle.reconstruct(
                        id,
                        List.of(event(
                                UUID.randomUUID(),
                                EquipmentModificationEventType.RECORDED,
                                INSTALLED
                        ))
                )
        );
    }

    @Test
    void rejectsRemovalBeforeInstallation() {
        UUID id = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentModificationLifecycle.reconstruct(
                        id,
                        List.of(
                                event(id,
                                        EquipmentModificationEventType.RECORDED,
                                        INSTALLED),
                                event(id,
                                        EquipmentModificationEventType.REMOVED,
                                        INSTALLED.minusSeconds(3600))
                        )
                )
        );
    }

    @Test
    void rejectsMissingInitialEvent() {
        UUID id = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentModificationLifecycle.reconstruct(
                        id,
                        List.of(event(
                                id,
                                EquipmentModificationEventType.REMOVED,
                                REMOVED
                        ))
                )
        );
    }

    @Test
    void acceptsEventsOutOfOrder() {
        UUID id = UUID.randomUUID();

        EquipmentModificationLifecycle lifecycle =
                EquipmentModificationLifecycle.reconstruct(
                        id,
                        List.of(
                                event(id,
                                        EquipmentModificationEventType.REMOVED,
                                        REMOVED),
                                event(id,
                                        EquipmentModificationEventType.RECORDED,
                                        INSTALLED)
                        )
                );

        assertEquals(INSTALLED, lifecycle.effectiveAt());
        assertEquals(REMOVED, lifecycle.removedAt());
    }

    @Test
    void rejectsNullEventList() {
        assertThrows(
                NullPointerException.class,
                () -> EquipmentModificationLifecycle.reconstruct(
                        UUID.randomUUID(), null
                )
        );
    }
}
