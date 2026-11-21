
package com.charron.api.cargo.domain;

import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CargoMeasurementsTest {

    @Test
    void acceptsWeightAndDimensions() {
        CargoMeasurements measurements =
                new CargoMeasurements(
                        new Weight(150000),
                        new Dimensions(1200, 800, 1000)
                );

        assertTrue(measurements.hasWeight());
        assertTrue(measurements.hasDimensions());
        assertEquals(150000, measurements.weight().grams());
    }

    @Test
    void acceptsWeightWithoutDimensions() {
        CargoMeasurements measurements =
                new CargoMeasurements(
                        new Weight(50000),
                        null
                );

        assertTrue(measurements.hasWeight());
        assertFalse(measurements.hasDimensions());
    }

    @Test
    void acceptsDimensionsWithoutWeight() {
        CargoMeasurements measurements =
                new CargoMeasurements(
                        null,
                        new Dimensions(1200, 800, 1000)
                );

        assertFalse(measurements.hasWeight());
        assertTrue(measurements.hasDimensions());
    }

    @Test
    void rejectsMissingMeasurements() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CargoMeasurements(null, null)
        );
    }

    @Test
    void supportsCargoWithPhysicalMeasurements() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Industrial Generator",
                "GEN-001",
                null
        );

        CargoMeasurements measurements =
                new CargoMeasurements(
                        new Weight(150000),
                        new Dimensions(1200, 800, 1000)
                );

        cargo.setMeasurements(measurements);

        assertEquals(
                measurements,
                cargo.getMeasurements()
        );
    }

    @Test
    void permitsCargoWithoutKnownMeasurements() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Unmeasured Freight",
                null,
                null
        );

        assertNull(cargo.getMeasurements());
    }

}
