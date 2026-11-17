
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


    @Test
    void configurationStartsWithoutCapabilities() {
        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Liftgate Configuration",
                        false
                );

        assertTrue(configuration.getCapabilities().isEmpty());
    }

    @Test
    void addsConfigurationCapabilities() {
        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Liftgate Configuration",
                        true
                );

        configuration.addCapability(
                EquipmentCapability.LIFTING
        );

        assertTrue(configuration.hasCapability(
                EquipmentCapability.LIFTING
        ));
    }

    @Test
    void preventsDuplicateConfigurationCapabilities() {
        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Refrigerated Configuration",
                        true
                );

        configuration.addCapability(
                EquipmentCapability.TEMPERATURE_CONTROL
        );

        configuration.addCapability(
                EquipmentCapability.TEMPERATURE_CONTROL
        );

        assertEquals(1, configuration.getCapabilities().size());
    }

    @Test
    void removesConfigurationCapabilities() {
        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Liftgate Configuration",
                        true
                );

        configuration.addCapability(
                EquipmentCapability.LIFTING
        );

        configuration.removeCapability(
                EquipmentCapability.LIFTING
        );

        assertFalse(configuration.hasCapability(
                EquipmentCapability.LIFTING
        ));
    }

    @Test
    void preventsExternalCapabilityModification() {
        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Liftgate Configuration",
                        true
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> configuration.getCapabilities().add(
                        EquipmentCapability.LIFTING
                )
        );
    }


    @Test
    void configurationStartsWithoutWeightAdjustment() {
        EquipmentConfiguration configuration =
                createWeightTestConfiguration();

        assertNull(configuration.getWeightAdjustment());
    }

    @Test
    void assignsWeightAdjustment() {
        EquipmentConfiguration configuration =
                createWeightTestConfiguration();

        ConfigurationWeightAdjustment adjustment =
                new ConfigurationWeightAdjustment(400_000);

        configuration.updateWeightAdjustment(adjustment);

        assertEquals(
                adjustment,
                configuration.getWeightAdjustment()
        );
    }

    @Test
    void replacesWeightAdjustment() {
        EquipmentConfiguration configuration =
                createWeightTestConfiguration();

        configuration.updateWeightAdjustment(
                new ConfigurationWeightAdjustment(400_000)
        );

        configuration.updateWeightAdjustment(
                new ConfigurationWeightAdjustment(-150_000)
        );

        assertEquals(
                new ConfigurationWeightAdjustment(-150_000),
                configuration.getWeightAdjustment()
        );
    }

    @Test
    void removesWeightAdjustmentWithoutChangingIdentity() {
        UUID id = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();

        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        id,
                        equipmentId,
                        "Liftgate Configuration",
                        true
                );

        configuration.updateWeightAdjustment(
                new ConfigurationWeightAdjustment(400_000)
        );

        configuration.removeWeightAdjustment();

        assertNull(configuration.getWeightAdjustment());
        assertEquals(id, configuration.getId());
        assertEquals(equipmentId, configuration.getEquipmentId());
    }

    @Test
    void rejectsNullWeightAdjustmentUpdate() {
        EquipmentConfiguration configuration =
                createWeightTestConfiguration();

        assertThrows(
                NullPointerException.class,
                () -> configuration.updateWeightAdjustment(null)
        );
    }

    private EquipmentConfiguration createWeightTestConfiguration() {
        return new EquipmentConfiguration(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Liftgate Configuration",
                true
        );
    }


}
