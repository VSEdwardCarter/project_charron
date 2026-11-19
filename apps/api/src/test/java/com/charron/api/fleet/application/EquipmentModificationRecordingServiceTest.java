
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Transactional
class EquipmentModificationRecordingServiceTest {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @MockitoSpyBean
    private EquipmentModificationRepository modificationRepository;

    @Autowired
    private EquipmentModificationRecordingService recordingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Instant EFFECTIVE_AT =
            Instant.parse("2026-11-10T10:00:00Z");

    private static final Instant RECORDED_AT =
            Instant.parse("2026-11-14T15:30:00Z");

    private UUID createEquipment() {
        UUID equipmentId = UUID.randomUUID();

        equipmentRepository.insertIdentity(new Equipment(
                equipmentId,
                "CHARRON Test Trailer",
                EquipmentCategory.TRAILER,
                null
        ));

        return equipmentId;
    }

    private EquipmentModification createModification(
            UUID equipmentId
    ) {
        return new EquipmentModification(
                UUID.randomUUID(),
                equipmentId,
                null,
                EquipmentModificationType.INSTALLATION,
                "Installed hydraulic liftgate",
                EFFECTIVE_AT,
                RECORDED_AT
        );
    }

    @Test
    void recordsModificationAndInitialEventTogether() {
        UUID equipmentId = createEquipment();

        EquipmentModification modification =
                createModification(equipmentId);

        EquipmentModificationEvent event =
                recordingService.record(modification);

        assertNotNull(event.id());
        assertEquals(modification.id(), event.modificationId());
        assertEquals(
                EquipmentModificationEventType.RECORDED,
                event.type()
        );
        assertEquals(EFFECTIVE_AT, event.effectiveAt());
        assertEquals(RECORDED_AT, event.recordedAt());

        assertEquals(
                modification,
                modificationRepository.findById(
                        modification.id()
                ).orElseThrow()
        );

        List<EquipmentModificationEvent> events =
                modificationRepository.findEventsByModificationId(
                        modification.id()
                );

        assertEquals(1, events.size());
        assertEquals(event, events.get(0));
    }

    @Test
    void rejectsAlreadyRemovedModification() {
        UUID equipmentId = createEquipment();

        EquipmentModification modification =
                new EquipmentModification(
                        UUID.randomUUID(),
                        equipmentId,
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed hydraulic liftgate",
                        EFFECTIVE_AT,
                        RECORDED_AT,
                        EFFECTIVE_AT.plusSeconds(86400)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> recordingService.record(modification)
        );

        assertTrue(
                modificationRepository.findById(
                        modification.id()
                ).isEmpty()
        );

        assertTrue(
                modificationRepository.findEventsByModificationId(
                        modification.id()
                ).isEmpty()
        );
    }

    @Test
    void rejectsNullModification() {
        assertThrows(
                NullPointerException.class,
                () -> recordingService.record(null)
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackModificationWhenEventInsertionFails() {
        UUID equipmentId = createEquipment();
        EquipmentModification modification =
                createModification(equipmentId);

        try {
            // Simulate failure of the second repository operation.
            doThrow(new IllegalStateException(
                    "Simulated event insertion failure"
            )).when(modificationRepository)
                    .insertEvent(any(EquipmentModificationEvent.class));

            assertThrows(
                    IllegalStateException.class,
                    () -> recordingService.record(modification)
            );

            // The first INSERT must have been rolled back.
            assertTrue(
                    modificationRepository.findById(
                            modification.id()
                    ).isEmpty()
            );

            assertTrue(
                    modificationRepository.findEventsByModificationId(
                            modification.id()
                    ).isEmpty()
            );
        } finally {
            // This test intentionally runs without an outer test
            // transaction, so remove its committed equipment fixture.
            jdbcTemplate.update(
                    "DELETE FROM equipment WHERE id = ?",
                    equipmentId
            );
        }
    }
}
