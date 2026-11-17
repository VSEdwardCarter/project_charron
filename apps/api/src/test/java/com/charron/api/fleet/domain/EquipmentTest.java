package com.charron.api.fleet.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentTest {

    @Test
    void createsEquipmentWithValidAttributes() {
        UUID id = UUID.randomUUID();

        Equipment equipment = new Equipment(
                id,
                "Tractor 001",
                EquipmentCategory.TRACTOR,
                "1HGCM82633A004352"
        );

        assertEquals(id, equipment.getId());
        assertEquals("Tractor 001", equipment.getName());
        assertEquals(EquipmentCategory.TRACTOR, equipment.getCategory());
        assertEquals("1HGCM82633A004352", equipment.getVin());
    }

    @Test
    void allowsEquipmentWithoutVin() {
        Equipment equipment = new Equipment(
                UUID.randomUUID(),
                "Warehouse Forklift",
                EquipmentCategory.MATERIAL_HANDLING,
                null
        );

        assertNull(equipment.getVin());
    }

    @Test
    void rejectsBlankEquipmentName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Equipment(
                        UUID.randomUUID(),
                        " ",
                        EquipmentCategory.TRAILER,
                        null
                )
        );
    }

    @Test
    void rejectsMissingEquipmentId() {
        assertThrows(
                NullPointerException.class,
                () -> new Equipment(
                        null,
                        "Trailer 001",
                        EquipmentCategory.TRAILER,
                        null
                )
        );
    }

    @Test
    void rejectsMissingCategory() {
        assertThrows(
                NullPointerException.class,
                () -> new Equipment(
                        UUID.randomUUID(),
                        "Equipment 001",
                        null,
                        null
                )
        );
    }

    @Test
    void normalizesVin() {
        Equipment equipment = new Equipment(
                UUID.randomUUID(),
                "Cargo Van 001",
                EquipmentCategory.CARGO_VAN,
                " 1hgcm82633a004352 "
        );

        assertEquals(
                "1HGCM82633A004352",
                equipment.getVin()
        );
    }


    @Test
    void createsEquipmentWithoutSpecifications() {
        Equipment equipment = new Equipment(
                UUID.randomUUID(),
                "Trailer 001",
                EquipmentCategory.TRAILER,
                null
        );

        assertNull(equipment.getSpecifications());
    }

    @Test
    void updatesSpecificationsWithoutChangingIdentity() {
        UUID equipmentId = UUID.randomUUID();

        Equipment equipment = new Equipment(
                equipmentId,
                "Trailer 001",
                EquipmentCategory.TRAILER,
                null
        );

        EquipmentSpecifications initial =
                new EquipmentSpecifications(
                        new Dimensions(12000, 2500, 4000)
                );

        EquipmentSpecifications updated =
                new EquipmentSpecifications(
                        new Dimensions(12500, 2600, 4000)
                );

        equipment.updateSpecifications(initial);
        assertEquals(initial, equipment.getSpecifications());

        equipment.updateSpecifications(updated);

        assertEquals(updated, equipment.getSpecifications());
        assertEquals(equipmentId, equipment.getId());
    }

    @Test
    void rejectsNullSpecificationUpdates() {
        Equipment equipment = new Equipment(
                UUID.randomUUID(),
                "Trailer 001",
                EquipmentCategory.TRAILER,
                null
        );

        assertThrows(
                NullPointerException.class,
                () -> equipment.updateSpecifications(null)
        );
    }

}