
package com.charron.api.fleet.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentSpecificationsTest {

    @Test
    void createsSpecificationsWithExteriorDimensions() {
        Dimensions dimensions = new Dimensions(
                12000, 2500, 4000
        );

        EquipmentSpecifications specifications =
                new EquipmentSpecifications(dimensions);

        assertEquals(
                dimensions,
                specifications.exteriorDimensions()
        );
    }

    @Test
    void rejectsMissingExteriorDimensions() {
        assertThrows(
                NullPointerException.class,
                () -> new EquipmentSpecifications(null)
        );
    }

    @Test
    void supportsValueEquality() {
        Dimensions dimensions = new Dimensions(
                12000, 2500, 4000
        );

        assertEquals(
                new EquipmentSpecifications(dimensions),
                new EquipmentSpecifications(dimensions)
        );
    }
}
