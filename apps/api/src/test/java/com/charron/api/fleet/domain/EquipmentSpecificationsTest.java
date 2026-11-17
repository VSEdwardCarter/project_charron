
package com.charron.api.fleet.domain;

import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentSpecificationsTest {

    @Test
    void supportsDimensionsAndWeight() {
        Dimensions dimensions =
                new Dimensions(12000, 2500, 4000);

        WeightSpecifications weights =
                new WeightSpecifications(
                        new Weight(7_000_000),
                        new Weight(36_000_000)
                );

        EquipmentSpecifications specifications =
                new EquipmentSpecifications(dimensions, weights);

        assertEquals(
                dimensions,
                specifications.exteriorDimensions()
        );

        assertEquals(
                weights,
                specifications.weightSpecifications()
        );
    }

    @Test
    void allowsDimensionsOnly() {
        EquipmentSpecifications specifications =
                new EquipmentSpecifications(
                        new Dimensions(12000, 2500, 4000),
                        null
                );

        assertNull(specifications.weightSpecifications());
    }

    @Test
    void allowsWeightOnly() {
        EquipmentSpecifications specifications =
                new EquipmentSpecifications(
                        null,
                        new WeightSpecifications(
                                new Weight(7_000_000),
                                new Weight(36_000_000)
                        )
                );

        assertNull(specifications.exteriorDimensions());
    }

    @Test
    void rejectsCompletelyEmptySpecifications() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentSpecifications(null, null)
        );
    }

    @Test
    void supportsValueEquality() {
        Dimensions dimensions =
                new Dimensions(12000, 2500, 4000);

        WeightSpecifications weights =
                new WeightSpecifications(
                        new Weight(7_000_000),
                        new Weight(36_000_000)
                );

        assertEquals(
                new EquipmentSpecifications(dimensions, weights),
                new EquipmentSpecifications(dimensions, weights)
        );
    }
}
