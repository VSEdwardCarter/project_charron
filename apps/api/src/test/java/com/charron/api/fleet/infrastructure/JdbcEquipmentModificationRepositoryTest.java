
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.application.EquipmentModificationRepository;
import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.domain.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JdbcEquipmentModificationRepositoryTest {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentModificationRepository modificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertsEquipmentModification() {
        UUID equipmentId = UUID.randomUUID();
        UUID modificationId = UUID.randomUUID();

        equipmentRepository.insertIdentity(new Equipment(
                equipmentId,
                "CHARRON Test Trailer",
                EquipmentCategory.TRAILER,
                null
        ));

        Instant effectiveAt =
                Instant.parse("2026-11-10T10:00:00Z");

        Instant recordedAt =
                Instant.parse("2026-11-14T15:30:00Z");

        EquipmentModification modification =
                new EquipmentModification(
                        modificationId,
                        equipmentId,
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed hydraulic liftgate",
                        effectiveAt,
                        recordedAt
                );

        modificationRepository.insert(modification);

        var stored = jdbcTemplate.queryForMap(
                """
                SELECT
                    equipment_id,
                    configuration_id,
                    type,
                    description,
                    effective_at,
                    recorded_at,
                    removed_at
                FROM equipment_modifications
                WHERE id = ?
                """,
                modificationId
        );

        assertEquals(
                equipmentId,
                stored.get("equipment_id")
        );

        assertNull(stored.get("configuration_id"));

        assertEquals(
                "INSTALLATION",
                stored.get("type")
        );

        assertEquals(
                "Installed hydraulic liftgate",
                stored.get("description")
        );

        assertEquals(
                effectiveAt,
                ((java.sql.Timestamp) stored.get("effective_at"))
                        .toInstant()
        );

        assertEquals(
                recordedAt,
                ((java.sql.Timestamp) stored.get("recorded_at"))
                        .toInstant()
        );

        assertNull(stored.get("removed_at"));
    }

    @Test
    void retrievesEquipmentModificationById() {
        UUID equipmentId = UUID.randomUUID();
        UUID modificationId = UUID.randomUUID();

        equipmentRepository.insertIdentity(new Equipment(
                equipmentId,
                "CHARRON Test Trailer",
                EquipmentCategory.TRAILER,
                null
        ));

        Instant effectiveAt =
                Instant.parse("2026-11-10T10:00:00Z");

        Instant recordedAt =
                Instant.parse("2026-11-14T15:30:00Z");

        Instant removedAt =
                Instant.parse("2026-11-16T12:00:00Z");

        EquipmentModification original =
                new EquipmentModification(
                        modificationId,
                        equipmentId,
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed hydraulic liftgate",
                        effectiveAt,
                        recordedAt,
                        removedAt
                );

        modificationRepository.insert(original);

        EquipmentModification retrieved =
                modificationRepository.findById(modificationId)
                        .orElseThrow();

        assertEquals(original, retrieved);
    }

    @Test
    void returnsEmptyWhenModificationDoesNotExist() {
        UUID nonexistentId = UUID.randomUUID();

        assertTrue(
                modificationRepository.findById(nonexistentId)
                        .isEmpty()
        );
    }


    @Test
    void insertsEquipmentModificationLifecycleEvent() {
        UUID equipmentId = UUID.randomUUID();
        UUID modificationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        equipmentRepository.insertIdentity(new Equipment(
                equipmentId,
                "CHARRON Test Trailer",
                EquipmentCategory.TRAILER,
                null
        ));

        Instant effectiveAt =
                Instant.parse("2026-11-10T10:00:00Z");

        Instant recordedAt =
                Instant.parse("2026-11-14T15:30:00Z");

        EquipmentModification modification =
                new EquipmentModification(
                        modificationId,
                        equipmentId,
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed hydraulic liftgate",
                        effectiveAt,
                        recordedAt
                );

        modificationRepository.insert(modification);

        EquipmentModificationEvent event =
                new EquipmentModificationEvent(
                        eventId,
                        modificationId,
                        EquipmentModificationEventType.RECORDED,
                        effectiveAt,
                        recordedAt
                );

        modificationRepository.insertEvent(event);

        var stored = jdbcTemplate.queryForMap(
                """
                SELECT
                    modification_id,
                    type,
                    effective_at,
                    recorded_at
                FROM equipment_modification_events
                WHERE id = ?
                """,
                eventId
        );

        assertEquals(
                modificationId,
                stored.get("modification_id")
        );

        assertEquals(
                "RECORDED",
                stored.get("type")
        );

        assertEquals(
                effectiveAt,
                ((java.sql.Timestamp) stored.get("effective_at"))
                        .toInstant()
        );

        assertEquals(
                recordedAt,
                ((java.sql.Timestamp) stored.get("recorded_at"))
                        .toInstant()
        );
    }


    @Test
    void retrievesModificationLifecycleEvents() {
        UUID equipmentId = UUID.randomUUID();
        UUID modificationId = UUID.randomUUID();

        equipmentRepository.insertIdentity(new Equipment(
                equipmentId,
                "CHARRON Test Trailer",
                EquipmentCategory.TRAILER,
                null
        ));

        Instant installedAt =
                Instant.parse("2026-11-10T10:00:00Z");

        Instant installationRecordedAt =
                Instant.parse("2026-11-14T15:30:00Z");

        EquipmentModification modification =
                new EquipmentModification(
                        modificationId,
                        equipmentId,
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed hydraulic liftgate",
                        installedAt,
                        installationRecordedAt
                );

        modificationRepository.insert(modification);

        EquipmentModificationEvent recordedEvent =
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        modificationId,
                        EquipmentModificationEventType.RECORDED,
                        installedAt,
                        installationRecordedAt
                );

        EquipmentModificationEvent removedEvent =
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        modificationId,
                        EquipmentModificationEventType.REMOVED,
                        Instant.parse("2026-11-15T12:00:00Z"),
                        Instant.parse("2026-11-18T09:00:00Z")
                );

        // Insert out of chronological order.
        modificationRepository.insertEvent(removedEvent);
        modificationRepository.insertEvent(recordedEvent);

        List<EquipmentModificationEvent> events =
                modificationRepository.findEventsByModificationId(
                        modificationId
                );

        assertEquals(
                List.of(recordedEvent, removedEvent),
                events
        );
    }


    @Test
    void returnsEmptyListWhenNoModificationEventsExist() {
        List<EquipmentModificationEvent> events =
                modificationRepository.findEventsByModificationId(
                        UUID.randomUUID()
                );

        assertTrue(events.isEmpty());
    }


    @Test
    void reconstructsHistoricalModificationLifecycle() {
        UUID equipmentId = UUID.randomUUID();
        UUID modificationId = UUID.randomUUID();

        equipmentRepository.insertIdentity(new Equipment(
                equipmentId,
                "CHARRON Test Trailer",
                EquipmentCategory.TRAILER,
                null
        ));

        Instant installedAt =
                Instant.parse("2026-11-10T10:00:00Z");

        Instant installationRecordedAt =
                Instant.parse("2026-11-14T15:30:00Z");

        Instant removedAt =
                Instant.parse("2026-11-15T12:00:00Z");

        Instant removalRecordedAt =
                Instant.parse("2026-11-18T09:00:00Z");

        EquipmentModification modification =
                new EquipmentModification(
                        modificationId,
                        equipmentId,
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed hydraulic liftgate",
                        installedAt,
                        installationRecordedAt
                );

        modificationRepository.insert(modification);

        modificationRepository.insertEvent(
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        modificationId,
                        EquipmentModificationEventType.RECORDED,
                        installedAt,
                        installationRecordedAt
                )
        );

        modificationRepository.insertEvent(
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        modificationId,
                        EquipmentModificationEventType.REMOVED,
                        removedAt,
                        removalRecordedAt
                )
        );

        // Retrieve persisted events from PostgreSQL.
        List<EquipmentModificationEvent> events =
                modificationRepository.findEventsByModificationId(
                        modificationId
                );

        // Reconstruct using the complete event history.
        EquipmentModificationLifecycle actualLifecycle =
                EquipmentModificationLifecycle.reconstruct(
                        modificationId,
                        events
                );

        Instant november16 =
                Instant.parse("2026-11-16T12:00:00Z");

        // Based on complete history, the liftgate was removed.
        assertFalse(
                actualLifecycle.isEffectiveAt(november16)
        );

        // Reconstruct using only information known on November 16.
        EquipmentModificationLifecycle knownLifecycle =
                EquipmentModificationLifecycle.reconstructAsKnownAt(
                        modificationId,
                        events,
                        november16
                );

        // On November 16, CHARRON had not recorded the removal.
        assertTrue(
                knownLifecycle.isEffectiveAt(november16)
        );

        // After the removal was recorded, knowledge changes.
        EquipmentModificationLifecycle updatedKnowledge =
                EquipmentModificationLifecycle.reconstructAsKnownAt(
                        modificationId,
                        events,
                        Instant.parse("2026-11-19T12:00:00Z")
                );

        assertFalse(
                updatedKnowledge.isEffectiveAt(november16)
        );
    }


}
