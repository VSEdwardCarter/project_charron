
package com.charron.api.fleet.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentModificationTest {

    private static final Instant EFFECTIVE =
            Instant.parse("2026-11-01T10:00:00Z");

    private static final Instant RECORDED =
            Instant.parse("2026-11-05T14:00:00Z");

    @Test
    void createsValidModification() {
        UUID id = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();
        UUID configurationId = UUID.randomUUID();

        EquipmentModification modification =
                new EquipmentModification(
                        id,
                        equipmentId,
                        configurationId,
                        EquipmentModificationType.INSTALLATION,
                        "Installed hydraulic liftgate",
                        EFFECTIVE,
                        RECORDED
                );

        assertEquals(id, modification.id());
        assertEquals(equipmentId, modification.equipmentId());
        assertEquals(configurationId, modification.configurationId());
        assertEquals(
                EquipmentModificationType.INSTALLATION,
                modification.type()
        );
        assertEquals(EFFECTIVE, modification.effectiveAt());
        assertEquals(RECORDED, modification.recordedAt());
    }

    @Test
    void allowsModificationWithoutConfiguration() {
        EquipmentModification modification =
                new EquipmentModification(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        EquipmentModificationType.ALTERATION,
                        "Structural reinforcement",
                        EFFECTIVE,
                        RECORDED
                );

        assertNull(modification.configurationId());
    }

    @Test
    void rejectsMissingEquipmentId() {
        assertThrows(
                NullPointerException.class,
                () -> new EquipmentModification(
                        UUID.randomUUID(),
                        null,
                        null,
                        EquipmentModificationType.REMOVAL,
                        "Removed liftgate",
                        EFFECTIVE,
                        RECORDED
                )
        );
    }

    @Test
    void rejectsBlankDescription() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentModification(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        EquipmentModificationType.REPLACEMENT,
                        " ",
                        EFFECTIVE,
                        RECORDED
                )
        );
    }

    @Test
    void normalizesDescription() {
        EquipmentModification modification =
                new EquipmentModification(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "  Installed liftgate  ",
                        EFFECTIVE,
                        RECORDED
                );

        assertEquals(
                "Installed liftgate",
                modification.description()
        );
    }

    @Test
    void permitsRetrospectiveRecording() {
        EquipmentModification modification =
                new EquipmentModification(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        EquipmentModificationType.ALTERATION,
                        "Modified cargo body",
                        EFFECTIVE,
                        RECORDED
                );

        assertTrue(
                modification.recordedAt()
                        .isAfter(modification.effectiveAt())
        );
    }

    @Test
    void supportsValueEquality() {
        UUID id = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();

        EquipmentModification first =
                new EquipmentModification(
                        id,
                        equipmentId,
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed liftgate",
                        EFFECTIVE,
                        RECORDED
                );

        EquipmentModification second =
                new EquipmentModification(
                        id,
                        equipmentId,
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed liftgate",
                        EFFECTIVE,
                        RECORDED
                );

        assertEquals(first, second);
    }


    @Test
    void allowsModificationWithoutRemovalDate() {
        EquipmentModification modification =
                new EquipmentModification(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed liftgate",
                        EFFECTIVE,
                        RECORDED,
                        null
                );

        assertNull(modification.removedAt());
        assertTrue(modification.isEffectiveAt(EFFECTIVE));
    }

    @Test
    void supportsHistoricalRemoval() {
        Instant removedAt = Instant.parse("2026-11-15T10:00:00Z");

        EquipmentModification modification =
                new EquipmentModification(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed liftgate",
                        EFFECTIVE,
                        RECORDED,
                        removedAt
                );

        assertTrue(modification.isEffectiveAt(EFFECTIVE));
        assertTrue(modification.isEffectiveAt(
                Instant.parse("2026-11-10T10:00:00Z")
        ));

        assertFalse(modification.isEffectiveAt(removedAt));
    }

    @Test
    void rejectsRemovalBeforeEffectiveDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentModification(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed liftgate",
                        EFFECTIVE,
                        RECORDED,
                        Instant.parse("2026-10-01T10:00:00Z")
                )
        );
    }

    @Test
    void allowsRemovalAtEffectiveDate() {
        EquipmentModification modification =
                new EquipmentModification(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        EquipmentModificationType.INSTALLATION,
                        "Installed liftgate",
                        EFFECTIVE,
                        RECORDED,
                        EFFECTIVE
                );

        assertFalse(modification.isEffectiveAt(EFFECTIVE));
    }

}
