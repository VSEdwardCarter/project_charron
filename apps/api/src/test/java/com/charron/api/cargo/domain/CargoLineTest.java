
package com.charron.api.cargo.domain;

import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CargoLineTest {

    private Cargo createCargo() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Industrial Generator",
                "GEN-001",
                null
        );

        cargo.setMeasurements(
                new CargoMeasurements(
                        new Weight(150000),
                        new Dimensions(1200, 800, 1000)
                )
        );

        return cargo;
    }

    @Test
    void calculatesTotalCargoWeight() {
        CargoLine line = new CargoLine(createCargo(), 12);

        assertEquals(
                1_800_000L,
                line.totalWeight().orElseThrow().grams()
        );
    }

    @Test
    void rejectsZeroOrNegativeQuantity() {
        Cargo cargo = createCargo();

        assertThrows(
                IllegalArgumentException.class,
                () -> new CargoLine(cargo, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new CargoLine(cargo, -1)
        );
    }

    @Test
    void rejectsMissingCargo() {
        assertThrows(
                NullPointerException.class,
                () -> new CargoLine(null, 1)
        );
    }

    @Test
    void preservesUnknownWeight() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Unmeasured Freight",
                null,
                null
        );

        CargoLine line = new CargoLine(cargo, 5);

        assertTrue(line.totalWeight().isEmpty());
    }

    @Test
    void rejectsWeightCalculationOverflow() {
        CargoLine line = new CargoLine(
                createCargo(),
                Long.MAX_VALUE
        );

        assertThrows(
                ArithmeticException.class,
                line::totalWeight
        );
    }
}
