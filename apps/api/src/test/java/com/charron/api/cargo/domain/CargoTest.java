
package com.charron.api.cargo.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CargoTest {

    @Test
    void createsCargoWithValidIdentity() {
        UUID id = UUID.randomUUID();

        Cargo cargo = new Cargo(
                id,
                "Industrial Generator",
                "GEN-001",
                "Portable industrial generator"
        );

        assertEquals(id, cargo.getId());
        assertEquals("Industrial Generator", cargo.getName());
        assertEquals("GEN-001", cargo.getReference());
        assertEquals(
                "Portable industrial generator",
                cargo.getDescription()
        );
    }

    @Test
    void normalizesCargoFields() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "  Steel Pallet  ",
                "  PALLET-123  ",
                "  Construction materials  "
        );

        assertEquals("Steel Pallet", cargo.getName());
        assertEquals("PALLET-123", cargo.getReference());
        assertEquals(
                "Construction materials",
                cargo.getDescription()
        );
    }

    @Test
    void allowsOptionalFieldsToBeMissing() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Equipment Parts",
                null,
                " "
        );

        assertNull(cargo.getReference());
        assertNull(cargo.getDescription());
    }

    @Test
    void rejectsMissingCargoIdentity() {
        assertThrows(
                NullPointerException.class,
                () -> new Cargo(
                        null,
                        "Generator",
                        null,
                        null
                )
        );
    }

    @Test
    void rejectsBlankCargoName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Cargo(
                        UUID.randomUUID(),
                        "   ",
                        null,
                        null
                )
        );
    }

    @Test
    void supportsUpdatingCargoDetails() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Original Cargo",
                null,
                null
        );

        cargo.setName("Updated Cargo");
        cargo.setReference("REF-200");
        cargo.setDescription("Updated description");

        assertEquals("Updated Cargo", cargo.getName());
        assertEquals("REF-200", cargo.getReference());
        assertEquals(
                "Updated description",
                cargo.getDescription()
        );
    }
}
