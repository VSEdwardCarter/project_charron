
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.application.EquipmentModificationRepository;
import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;
import com.charron.api.fleet.domain.EquipmentModification;
import com.charron.api.fleet.domain.EquipmentModificationType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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


}
