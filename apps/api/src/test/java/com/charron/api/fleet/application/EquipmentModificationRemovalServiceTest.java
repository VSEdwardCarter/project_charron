
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.EquipmentModification;
import com.charron.api.fleet.domain.EquipmentModificationEvent;
import com.charron.api.fleet.domain.EquipmentModificationEventType;
import com.charron.api.fleet.domain.EquipmentModificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EquipmentModificationRemovalServiceTest {

    private static final Instant INSTALLED_AT =
            Instant.parse("2026-08-01T12:00:00Z");

    @Autowired
    private EquipmentModificationRecordingService recordingService;

    @Autowired
    private EquipmentModificationRemovalService removalService;

    @Autowired
    private EquipmentModificationRepository repository;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Test
    void removesModificationAndRecordsLifecycleEvent() {
        UUID modificationId = createModification();

        Instant removedAt =
                Instant.parse("2026-11-01T12:00:00Z");

        EquipmentModificationEvent event =
                removalService.remove(modificationId, removedAt);

        assertEquals(
                EquipmentModificationEventType.REMOVED,
                event.type()
        );

        assertEquals(removedAt, event.effectiveAt());
        assertNotNull(event.recordedAt());

        EquipmentModification modification =
                repository.findById(modificationId).orElseThrow();

        assertEquals(removedAt, modification.removedAt());

        var events =
                repository.findEventsByModificationId(modificationId);

        assertEquals(2, events.size());

        assertTrue(events.stream().anyMatch(e ->
                e.type() == EquipmentModificationEventType.RECORDED
        ));

        assertTrue(events.stream().anyMatch(e ->
                e.type() == EquipmentModificationEventType.REMOVED
                        && e.effectiveAt().equals(removedAt)
        ));
    }

    @Test
    void rejectsDuplicateRemoval() {
        UUID modificationId = createModification();

        removalService.remove(
                modificationId,
                Instant.parse("2026-10-01T12:00:00Z")
        );

        assertThrows(
                IllegalStateException.class,
                () -> removalService.remove(
                        modificationId,
                        Instant.parse("2026-11-01T12:00:00Z")
                )
        );

        assertEquals(
                2,
                repository.findEventsByModificationId(
                        modificationId
                ).size()
        );
    }

    @Test
    void rejectsRemovalBeforeInstallation() {
        UUID modificationId = createModification();

        assertThrows(
                IllegalArgumentException.class,
                () -> removalService.remove(
                        modificationId,
                        Instant.parse("2026-07-01T12:00:00Z")
                )
        );

        assertNull(
                repository.findById(modificationId)
                        .orElseThrow()
                        .removedAt()
        );

        assertEquals(
                1,
                repository.findEventsByModificationId(
                        modificationId
                ).size()
        );
    }

    private UUID createModification() {
        UUID equipmentId = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO equipment (id, name, category, vin)
                VALUES (?, ?, ?, ?)
                """,
                equipmentId,
                "Modification Removal Test",
                "OTHER",
                null
        );

        EquipmentModification modification =
                new EquipmentModification(
                        UUID.randomUUID(),
                        equipmentId,
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed auxiliary lighting",
                        INSTALLED_AT,
                        Instant.parse("2026-09-01T12:00:00Z")
                );

        recordingService.record(modification);

        return modification.id();
    }
}
