
package com.charron.api.fleet.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentModificationEventTest {

    private static final Instant EFFECTIVE =
            Instant.parse("2026-11-01T10:00:00Z");

    private static final Instant RECORDED =
            Instant.parse("2026-11-05T14:00:00Z");

    @Test
    void createsValidEvent() {
        UUID id = UUID.randomUUID();
        UUID modificationId = UUID.randomUUID();

        EquipmentModificationEvent event =
                new EquipmentModificationEvent(
                        id,
                        modificationId,
                        EquipmentModificationEventType.RECORDED,
                        EFFECTIVE,
                        RECORDED
                );

        assertEquals(id, event.id());
        assertEquals(modificationId, event.modificationId());
        assertEquals(
                EquipmentModificationEventType.RECORDED,
                event.type()
        );
        assertEquals(EFFECTIVE, event.effectiveAt());
        assertEquals(RECORDED, event.recordedAt());
    }

    @Test
    void supportsRemovalEvent() {
        EquipmentModificationEvent event =
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        EquipmentModificationEventType.REMOVED,
                        Instant.parse("2026-11-15T10:00:00Z"),
                        Instant.parse("2026-11-16T09:00:00Z")
                );

        assertEquals(
                EquipmentModificationEventType.REMOVED,
                event.type()
        );
    }

    @Test
    void permitsRetrospectiveRecording() {
        EquipmentModificationEvent event =
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        EquipmentModificationEventType.RECORDED,
                        EFFECTIVE,
                        RECORDED
                );

        assertTrue(event.recordedAt().isAfter(event.effectiveAt()));
    }

    @Test
    void rejectsMissingEventId() {
        assertThrows(
                NullPointerException.class,
                () -> new EquipmentModificationEvent(
                        null,
                        UUID.randomUUID(),
                        EquipmentModificationEventType.RECORDED,
                        EFFECTIVE,
                        RECORDED
                )
        );
    }

    @Test
    void rejectsMissingModificationId() {
        assertThrows(
                NullPointerException.class,
                () -> new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        null,
                        EquipmentModificationEventType.RECORDED,
                        EFFECTIVE,
                        RECORDED
                )
        );
    }

    @Test
    void rejectsMissingEventType() {
        assertThrows(
                NullPointerException.class,
                () -> new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        EFFECTIVE,
                        RECORDED
                )
        );
    }

    @Test
    void supportsValueEquality() {
        UUID id = UUID.randomUUID();
        UUID modificationId = UUID.randomUUID();

        EquipmentModificationEvent first =
                new EquipmentModificationEvent(
                        id,
                        modificationId,
                        EquipmentModificationEventType.RECORDED,
                        EFFECTIVE,
                        RECORDED
                );

        EquipmentModificationEvent second =
                new EquipmentModificationEvent(
                        id,
                        modificationId,
                        EquipmentModificationEventType.RECORDED,
                        EFFECTIVE,
                        RECORDED
                );

        assertEquals(first, second);
    }
}
