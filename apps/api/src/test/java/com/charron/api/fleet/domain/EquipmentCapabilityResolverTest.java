
package com.charron.api.fleet.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentCapabilityResolverTest {

    @Test
    void resolvesBaseCapabilitiesWithoutConfiguration() {
        Equipment equipment = createEquipment();

        equipment.addCapability(
                EquipmentCapability.PROPULSION
        );

        Set<EquipmentCapability> effective =
                EquipmentCapabilityResolver.resolve(
                        equipment, null
                );

        assertEquals(
                Set.of(EquipmentCapability.PROPULSION),
                effective
        );
    }

    @Test
    void combinesBaseAndConfigurationCapabilities() {
        Equipment equipment = createEquipment();

        equipment.addCapability(
                EquipmentCapability.CARGO_TRANSPORT
        );

        EquipmentConfiguration configuration =
                createConfiguration(equipment, true);

        configuration.addCapability(
                EquipmentCapability.LIFTING
        );

        Set<EquipmentCapability> effective =
                EquipmentCapabilityResolver.resolve(
                        equipment, configuration
                );

        assertEquals(
                Set.of(
                        EquipmentCapability.CARGO_TRANSPORT,
                        EquipmentCapability.LIFTING
                ),
                effective
        );
    }

    @Test
    void eliminatesDuplicateCapabilities() {
        Equipment equipment = createEquipment();

        equipment.addCapability(
                EquipmentCapability.CARGO_TRANSPORT
        );

        EquipmentConfiguration configuration =
                createConfiguration(equipment, true);

        configuration.addCapability(
                EquipmentCapability.CARGO_TRANSPORT
        );

        Set<EquipmentCapability> effective =
                EquipmentCapabilityResolver.resolve(
                        equipment, configuration
                );

        assertEquals(1, effective.size());
    }

    @Test
    void rejectsConfigurationBelongingToAnotherEquipment() {
        Equipment equipment = createEquipment();

        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Unrelated Configuration",
                        true
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentCapabilityResolver.resolve(
                        equipment, configuration
                )
        );
    }

    @Test
    void rejectsInactiveConfiguration() {
        Equipment equipment = createEquipment();

        EquipmentConfiguration configuration =
                createConfiguration(equipment, false);

        assertThrows(
                IllegalStateException.class,
                () -> EquipmentCapabilityResolver.resolve(
                        equipment, configuration
                )
        );
    }

    @Test
    void preventsModificationOfResolvedCapabilities() {
        Equipment equipment = createEquipment();

        Set<EquipmentCapability> effective =
                EquipmentCapabilityResolver.resolve(
                        equipment, null
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> effective.add(
                        EquipmentCapability.TOWING
                )
        );
    }

    @Test
    void doesNotModifyOriginalCapabilityCollections() {
        Equipment equipment = createEquipment();

        equipment.addCapability(
                EquipmentCapability.PROPULSION
        );

        EquipmentConfiguration configuration =
                createConfiguration(equipment, true);

        configuration.addCapability(
                EquipmentCapability.LIFTING
        );

        EquipmentCapabilityResolver.resolve(
                equipment, configuration
        );

        assertEquals(
                Set.of(EquipmentCapability.PROPULSION),
                equipment.getCapabilities()
        );

        assertEquals(
                Set.of(EquipmentCapability.LIFTING),
                configuration.getCapabilities()
        );
    }

    @Test
    void rejectsNullEquipment() {
        assertThrows(
                NullPointerException.class,
                () -> EquipmentCapabilityResolver.resolve(
                        null, null
                )
        );
    }

    private Equipment createEquipment() {
        return new Equipment(
                UUID.randomUUID(),
                "Straight Truck 001",
                EquipmentCategory.STRAIGHT_TRUCK,
                null
        );
    }

    private EquipmentConfiguration createConfiguration(
            Equipment equipment,
            boolean active
    ) {
        return new EquipmentConfiguration(
                UUID.randomUUID(),
                equipment.getId(),
                "Liftgate Configuration",
                active
        );
    }
}
