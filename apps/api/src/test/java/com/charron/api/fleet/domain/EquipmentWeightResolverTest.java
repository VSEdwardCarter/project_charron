
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

    private Equipment createEquipmentWithPayload(
            long emptyWeightGrams,
            Long grossWeightRatingGrams,
            Long ratedPayloadGrams
    ) {
        Equipment equipment = new Equipment(
                UUID.randomUUID(),
                "Payload Test Trailer",
                EquipmentCategory.TRAILER,
                null
        );

        equipment.updateSpecifications(
                new EquipmentSpecifications(
                        null,
                        new WeightSpecifications(
                                new Weight(emptyWeightGrams),
                                grossWeightRatingGrams == null
                                        ? null
                                        : new Weight(grossWeightRatingGrams),
                                ratedPayloadGrams == null
                                        ? null
                                        : new Weight(ratedPayloadGrams)
                        )
                )
        );

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

    @Test
    void availablePayloadDecreasesWithAddedConfigurationWeight() {
        Equipment equipment = createEquipmentWithPayload(
                2_000_000L,
                3_000_000L,
                800_000L
        );

        EquipmentConfiguration configuration = createConfiguration(
                equipment,
                true,
                new ConfigurationWeightAdjustment(100_000L)
        );

        assertEquals(
                new Weight(700_000L),
                EquipmentWeightResolver.resolveAvailablePayload(
                        equipment, configuration
                )
        );
    }

    @Test
    void availablePayloadIncreasesWithReducedConfigurationWeight() {
        Equipment equipment = createEquipmentWithPayload(
                2_000_000L,
                3_000_000L,
                800_000L
        );

        EquipmentConfiguration configuration = createConfiguration(
                equipment,
                true,
                new ConfigurationWeightAdjustment(-100_000L)
        );

        assertEquals(
                new Weight(900_000L),
                EquipmentWeightResolver.resolveAvailablePayload(
                        equipment, configuration
                )
        );
    }

    @Test
    void availablePayloadUsesGrossWeightAllowanceWhenMoreRestrictive() {
        Equipment equipment = createEquipmentWithPayload(
                2_000_000L,
                3_000_000L,
                950_000L
        );

        EquipmentConfiguration configuration = createConfiguration(
                equipment,
                true,
                new ConfigurationWeightAdjustment(100_000L)
        );

        assertEquals(
                new Weight(850_000L),
                EquipmentWeightResolver.resolveAvailablePayload(
                        equipment, configuration
                )
        );
    }

    @Test
    void availablePayloadUsesGrossRatingWhenRatedPayloadUnknown() {
        Equipment equipment = createEquipmentWithPayload(
                2_000_000L,
                3_000_000L,
                null
        );

        assertEquals(
                new Weight(1_000_000L),
                EquipmentWeightResolver.resolveAvailablePayload(
                        equipment, null
                )
        );
    }

    @Test
    void availablePayloadUsesRatedPayloadWhenGrossRatingUnknown() {
        Equipment equipment = createEquipmentWithPayload(
                2_000_000L,
                null,
                800_000L
        );

        EquipmentConfiguration configuration = createConfiguration(
                equipment,
                true,
                new ConfigurationWeightAdjustment(100_000L)
        );

        assertEquals(
                new Weight(700_000L),
                EquipmentWeightResolver.resolveAvailablePayload(
                        equipment, configuration
                )
        );
    }

    @Test
    void availablePayloadCannotBecomeNegative() {
        Equipment equipment = createEquipmentWithPayload(
                2_000_000L,
                3_000_000L,
                800_000L
        );

        EquipmentConfiguration configuration = createConfiguration(
                equipment,
                true,
                new ConfigurationWeightAdjustment(900_000L)
        );

        assertEquals(
                new Weight(0L),
                EquipmentWeightResolver.resolveAvailablePayload(
                        equipment, configuration
                )
        );
    }

    @Test
    void rejectsAvailablePayloadCalculationWithoutCapacityLimits() {
        Equipment equipment = createEquipmentWithPayload(
                2_000_000L,
                null,
                null
        );

        assertThrows(
                IllegalStateException.class,
                () -> EquipmentWeightResolver.resolveAvailablePayload(
                        equipment, null
                )
        );
    }

    @Test
    void availablePayloadCalculationPreservesBaselineSpecifications() {
        Equipment equipment = createEquipmentWithPayload(
                2_000_000L,
                3_000_000L,
                800_000L
        );

        EquipmentConfiguration configuration = createConfiguration(
                equipment,
                true,
                new ConfigurationWeightAdjustment(100_000L)
        );

        Weight availablePayload =
                EquipmentWeightResolver.resolveAvailablePayload(
                        equipment, configuration
                );

        assertEquals(new Weight(700_000L), availablePayload);

        WeightSpecifications original =
                equipment.getSpecifications().weightSpecifications();

        assertEquals(new Weight(2_000_000L), original.emptyWeight());
        assertEquals(new Weight(3_000_000L), original.grossWeightRating());
        assertEquals(new Weight(800_000L), original.ratedPayloadCapacity());

        assertEquals(
                new ConfigurationWeightAdjustment(100_000L),
                configuration.getWeightAdjustment()
        );
    }


}
