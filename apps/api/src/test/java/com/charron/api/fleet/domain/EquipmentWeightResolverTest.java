
package com.charron.api.fleet.domain;

import com.charron.api.common.measurement.Weight;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentWeightResolverTest {

    private Equipment createEquipment(
            Weight emptyWeight,
            Weight grossWeightRating
    ) {
        Equipment equipment = new Equipment(
                UUID.randomUUID(),
                "Trailer 001",
                EquipmentCategory.TRAILER,
                null
        );

        if (emptyWeight != null || grossWeightRating != null) {
            equipment.updateSpecifications(
                    new EquipmentSpecifications(
                            null,
                            new WeightSpecifications(
                                    emptyWeight,
                                    grossWeightRating
                            )
                    )
            );
        }

        return equipment;
    }

    private EquipmentConfiguration createConfiguration(
            Equipment equipment,
            boolean active,
            ConfigurationWeightAdjustment adjustment
    ) {
        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        UUID.randomUUID(),
                        equipment.getId(),
                        "Modified Trailer",
                        active
                );

        if (adjustment != null) {
            configuration.updateWeightAdjustment(adjustment);
        }

        return configuration;
    }

    @Test
    void returnsBaseWeightWithoutConfiguration() {
        Equipment equipment = createEquipment(
                new Weight(6_000_000),
                new Weight(16_000_000)
        );

        assertEquals(
                new Weight(6_000_000),
                EquipmentWeightResolver.resolveEmptyWeight(
                        equipment, null
                )
        );
    }

    @Test
    void appliesPositiveConfigurationAdjustment() {
        Equipment equipment = createEquipment(
                new Weight(6_000_000),
                new Weight(16_000_000)
        );

        EquipmentConfiguration configuration =
                createConfiguration(
                        equipment,
                        true,
                        new ConfigurationWeightAdjustment(400_000)
                );

        assertEquals(
                new Weight(6_400_000),
                EquipmentWeightResolver.resolveEmptyWeight(
                        equipment, configuration
                )
        );
    }

    @Test
    void appliesNegativeConfigurationAdjustment() {
        Equipment equipment = createEquipment(
                new Weight(6_000_000),
                new Weight(16_000_000)
        );

        EquipmentConfiguration configuration =
                createConfiguration(
                        equipment,
                        true,
                        new ConfigurationWeightAdjustment(-150_000)
                );

        assertEquals(
                new Weight(5_850_000),
                EquipmentWeightResolver.resolveEmptyWeight(
                        equipment, configuration
                )
        );
    }

    @Test
    void rejectsConfigurationFromDifferentEquipment() {
        Equipment equipment = createEquipment(
                new Weight(6_000_000),
                new Weight(16_000_000)
        );

        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Other Trailer",
                        true
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentWeightResolver.resolveEmptyWeight(
                        equipment, configuration
                )
        );
    }

    @Test
    void rejectsInactiveConfiguration() {
        Equipment equipment = createEquipment(
                new Weight(6_000_000),
                new Weight(16_000_000)
        );

        EquipmentConfiguration configuration =
                createConfiguration(equipment, false, null);

        assertThrows(
                IllegalStateException.class,
                () -> EquipmentWeightResolver.resolveEmptyWeight(
                        equipment, configuration
                )
        );
    }

    @Test
    void rejectsUnknownBaseWeight() {
        Equipment equipment = createEquipment(
                null,
                new Weight(16_000_000)
        );

        assertThrows(
                IllegalStateException.class,
                () -> EquipmentWeightResolver.resolveEmptyWeight(
                        equipment, null
                )
        );
    }

    @Test
    void rejectsAdjustedWeightAboveGrossRating() {
        Equipment equipment = createEquipment(
                new Weight(6_000_000),
                new Weight(6_200_000)
        );

        EquipmentConfiguration configuration =
                createConfiguration(
                        equipment,
                        true,
                        new ConfigurationWeightAdjustment(400_000)
                );

        assertThrows(
                IllegalStateException.class,
                () -> EquipmentWeightResolver.resolveEmptyWeight(
                        equipment, configuration
                )
        );
    }

    @Test
    void preservesOriginalBaseWeight() {
        Equipment equipment = createEquipment(
                new Weight(6_000_000),
                new Weight(16_000_000)
        );

        EquipmentConfiguration configuration =
                createConfiguration(
                        equipment,
                        true,
                        new ConfigurationWeightAdjustment(400_000)
                );

        EquipmentWeightResolver.resolveEmptyWeight(
                equipment, configuration
        );

        assertEquals(
                new Weight(6_000_000),
                equipment.getSpecifications()
                        .weightSpecifications()
                        .emptyWeight()
        );

        assertEquals(
                new ConfigurationWeightAdjustment(400_000),
                configuration.getWeightAdjustment()
        );
    }

    @Test
    void rejectsNullEquipment() {
        assertThrows(
                NullPointerException.class,
                () -> EquipmentWeightResolver.resolveEmptyWeight(
                        null, null
                )
        );
    }

    @Test
    void returnsBaseWhenNoAdjustmentSpecified() {
        Equipment equipment = createEquipment(
                new Weight(6_000_000),
                new Weight(16_000_000)
        );

        EquipmentConfiguration configuration =
                createConfiguration(equipment, true, null);

        assertEquals(
                new Weight(6_000_000),
                EquipmentWeightResolver.resolveEmptyWeight(
                        equipment, configuration
                )
        );
    }
}
