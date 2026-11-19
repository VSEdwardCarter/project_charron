
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.application.EquipmentModificationRecordingService;
import com.charron.api.fleet.application.EquipmentModificationRepository;
import com.charron.api.fleet.domain.EquipmentModification;
import com.charron.api.fleet.domain.EquipmentModificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JdbcEquipmentModificationHistoryTest {

    @Autowired
    private EquipmentModificationRepository repository;

    @Autowired
    private EquipmentModificationRecordingService recordingService;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Test
    void retrievesOnlyEquipmentModificationsInEffectiveDateOrder() {
        UUID firstEquipmentId = UUID.randomUUID();
        UUID secondEquipmentId = UUID.randomUUID();

        insertEquipment(firstEquipmentId, "History Test Equipment A");
        insertEquipment(secondEquipmentId, "History Test Equipment B");

        Instant recordedAt = Instant.parse("2026-11-19T18:00:00Z");

        EquipmentModification older = new EquipmentModification(
                UUID.randomUUID(),
                firstEquipmentId,
                null,
                EquipmentModificationType.INSTALLATION,
                "Installed auxiliary lighting",
                Instant.parse("2026-08-01T12:00:00Z"),
                recordedAt
        );

        EquipmentModification newer = new EquipmentModification(
                UUID.randomUUID(),
                firstEquipmentId,
                null,
                EquipmentModificationType.REPLACEMENT,
                "Replaced alternator",
                Instant.parse("2026-10-15T12:00:00Z"),
                recordedAt
        );

        EquipmentModification unrelated = new EquipmentModification(
                UUID.randomUUID(),
                secondEquipmentId,
                null,
                EquipmentModificationType.ALTERATION,
                "Modified cargo storage",
                Instant.parse("2026-09-10T12:00:00Z"),
                recordedAt
        );

        recordingService.record(older);
        recordingService.record(newer);
        recordingService.record(unrelated);

        List<EquipmentModification> history =
                repository.findByEquipmentId(firstEquipmentId);

        assertEquals(2, history.size());

        // Newest effective modification first.
        assertEquals(newer.id(), history.get(0).id());
        assertEquals(older.id(), history.get(1).id());

        // No modifications from another equipment asset.
        assertTrue(history.stream().allMatch(
                modification ->
                        firstEquipmentId.equals(modification.equipmentId())
        ));

        // Preserve the difference between effective and recorded dates.
        assertEquals(
                Instant.parse("2026-08-01T12:00:00Z"),
                history.get(1).effectiveAt()
        );

        assertEquals(recordedAt, history.get(1).recordedAt());
    }

    private void insertEquipment(UUID id, String name) {
        jdbcTemplate.update("""
                INSERT INTO equipment (id, name, category, vin)
                VALUES (?, ?, ?, ?)
                """,
                id,
                name,
                "OTHER",
                null
        );
    }
}
