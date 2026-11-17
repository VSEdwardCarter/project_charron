
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


    @Test
    void reconstructsLifecycleBeforeRemovalWasRecorded() {
        UUID id = UUID.randomUUID();

        EquipmentModificationEvent installation =
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        id,
                        EquipmentModificationEventType.RECORDED,
                        INSTALLED,
                        Instant.parse("2026-11-05T10:00:00Z")
                );

        EquipmentModificationEvent removal =
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        id,
                        EquipmentModificationEventType.REMOVED,
                        REMOVED,
                        Instant.parse("2026-11-16T10:00:00Z")
                );

        EquipmentModificationLifecycle lifecycle =
                EquipmentModificationLifecycle.reconstructAsKnownAt(
                        id,
                        List.of(installation, removal),
                        Instant.parse("2026-11-15T12:00:00Z")
                );

        assertNull(lifecycle.removedAt());
        assertTrue(lifecycle.isEffectiveAt(
                Instant.parse("2026-11-15T12:00:00Z")
        ));
    }

    @Test
    void reconstructsRemovalAfterItWasRecorded() {
        UUID id = UUID.randomUUID();

        EquipmentModificationEvent installation =
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        id,
                        EquipmentModificationEventType.RECORDED,
                        INSTALLED,
                        Instant.parse("2026-11-05T10:00:00Z")
                );

        EquipmentModificationEvent removal =
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        id,
                        EquipmentModificationEventType.REMOVED,
                        REMOVED,
                        Instant.parse("2026-11-16T10:00:00Z")
                );

        EquipmentModificationLifecycle lifecycle =
                EquipmentModificationLifecycle.reconstructAsKnownAt(
                        id,
                        List.of(installation, removal),
                        Instant.parse("2026-11-17T10:00:00Z")
                );

        assertEquals(REMOVED, lifecycle.removedAt());
        assertFalse(lifecycle.isEffectiveAt(REMOVED));
    }

    @Test
    void includesEventsRecordedExactlyAtKnownTime() {
        UUID id = UUID.randomUUID();

        Instant recordedAt =
                Instant.parse("2026-11-05T10:00:00Z");

        EquipmentModificationLifecycle lifecycle =
                EquipmentModificationLifecycle.reconstructAsKnownAt(
                        id,
                        List.of(new EquipmentModificationEvent(
                                UUID.randomUUID(),
                                id,
                                EquipmentModificationEventType.RECORDED,
                                INSTALLED,
                                recordedAt
                        )),
                        recordedAt
                );

        assertEquals(INSTALLED, lifecycle.effectiveAt());
    }

    @Test
    void rejectsReconstructionBeforeInitialRecording() {
        UUID id = UUID.randomUUID();

        EquipmentModificationEvent installation =
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        id,
                        EquipmentModificationEventType.RECORDED,
                        INSTALLED,
                        Instant.parse("2026-11-05T10:00:00Z")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentModificationLifecycle.reconstructAsKnownAt(
                        id,
                        List.of(installation),
                        Instant.parse("2026-11-04T10:00:00Z")
                )
        );
    }

    @Test
    void rejectsNullKnownAtTimestamp() {
        assertThrows(
                NullPointerException.class,
                () -> EquipmentModificationLifecycle.reconstructAsKnownAt(
                        UUID.randomUUID(),
                        List.of(),
                        null
                )
        );
    }

}
