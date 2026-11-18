
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;
import com.charron.api.fleet.domain.EquipmentModification;
import com.charron.api.fleet.domain.EquipmentModificationEvent;
import com.charron.api.fleet.domain.EquipmentModificationEventType;
import com.charron.api.fleet.domain.EquipmentModificationType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class EquipmentModificationHistoryServiceTest {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentModificationRepository modificationRepository;

    @Autowired
    private EquipmentModificationHistoryService historyService;

    @Test
    void distinguishesPhysicalHistoryFromRecordedKnowledge() {
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

        modificationRepository.insert(
                new EquipmentModification(
                        modificationId,
                        equipmentId,
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed hydraulic liftgate",
                        installedAt,
                        installationRecordedAt
                )
        );

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

        Instant queryTime =
                Instant.parse("2026-11-16T12:00:00Z");

        // Complete history: the liftgate had been removed.
        assertFalse(
                historyService.isEffectiveAt(
                        modificationId,
                        queryTime
                )
        );

        // Knowledge on November 16: removal not yet recorded.
        assertTrue(
                historyService.isEffectiveAsKnownAt(
                        modificationId,
                        queryTime,
                        queryTime
                )
        );

        // Knowledge after November 18: removal is now known.
        assertFalse(
                historyService.isEffectiveAsKnownAt(
                        modificationId,
                        queryTime,
                        Instant.parse("2026-11-19T12:00:00Z")
                )
        );
    }
}
