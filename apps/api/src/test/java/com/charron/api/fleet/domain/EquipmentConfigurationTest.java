
package com.charron.api.fleet.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentConfigurationTest {

    @Test
    void createsValidConfiguration() {
        UUID id = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();

        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        id,
                        equipmentId,
                        "Standard Cargo Configuration",
                        false
                );

        assertEquals(id, configuration.getId());
        assertEquals(equipmentId, configuration.getEquipmentId());
        assertEquals(
                "Standard Cargo Configuration",
                configuration.getName()
        );
        assertFalse(configuration.isActive());
    }

    @Test
    void rejectsMissingConfigurationId() {
        assertThrows(
                NullPointerException.class,
                () -> new EquipmentConfiguration(
                        null,
                        UUID.randomUUID(),
                        "Standard",
                        false
                )
        );
    }

    @Test
    void rejectsMissingEquipmentId() {
        assertThrows(
                NullPointerException.class,
                () -> new EquipmentConfiguration(
                        UUID.randomUUID(),
                        null,
                        "Standard",
                        false
                )
        );
    }

    @Test
    void rejectsBlankConfigurationName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentConfiguration(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        " ",
                        false
                )
        );
    }

    @Test
    void trimsConfigurationName() {
        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "  Refrigerated Configuration  ",
                        false
                );

        assertEquals(
                "Refrigerated Configuration",
                configuration.getName()
        );
    }

    @Test
    void activatesConfiguration() {
        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Standard",
                        false
                );

        configuration.activate();

        assertTrue(configuration.isActive());
    }

    @Test
    void deactivatesConfiguration() {
        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Standard",
                        true
                );

        configuration.deactivate();

        assertFalse(configuration.isActive());
    }

    @Test
    void renamesConfigurationWithoutChangingIdentity() {
        UUID id = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();

        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        id,
                        equipmentId,
                        "Original",
                        false
                );

        configuration.setName("Updated");

        assertEquals("Updated", configuration.getName());
        assertEquals(id, configuration.getId());
        assertEquals(
                equipmentId,
                configuration.getEquipmentId()
        );
    }
}
