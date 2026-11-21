
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.Cargo;
import com.charron.api.cargo.domain.CargoLine;
import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;
import com.charron.api.fleet.domain.CargoSpaceType;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CargoLineSpatialFeasibilityTest {

    private final CargoSpaceSpecifications space =
            new CargoSpaceSpecifications(
                    CargoSpaceType.ENCLOSED,
                    7000,
                    2400,
                    2500L
            );

    private CargoLine line(
            long length,
            long width,
            long height,
            long quantity
    ) {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Test Cargo",
                null,
                null
        );

        cargo.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(length, width, height)
                )
        );

        return new CargoLine(cargo, quantity);
    }

    @Test
    void calculatesBothGridCapacities() {
        var result = CargoLineSpatialFeasibility.evaluate(
                line(1200, 800, 1000, 10),
                space
        );

        assertEquals(15L, result.originalCapacity());
        assertEquals(16L, result.rotatedCapacity());

        assertEquals(
                CargoLineSpatialFeasibility.Status.FITS_IN_GRID,
                result.status()
        );

        assertEquals(
                CargoSpatialFeasibility.Orientation.ROTATED_90,
                result.orientation()
        );
    }

    @Test
    void fitsExactlyAtGridCapacity() {
        var result = CargoLineSpatialFeasibility.evaluate(
                line(1200, 800, 1000, 16),
                space
        );

        assertEquals(
                CargoLineSpatialFeasibility.Status.FITS_IN_GRID,
                result.status()
        );
    }

    @Test
    void reportsWhenQuantityExceedsGridCapacity() {
        var result = CargoLineSpatialFeasibility.evaluate(
                line(1200, 800, 1000, 17),
                space
        );

        assertEquals(
                CargoLineSpatialFeasibility.Status.NOT_FIT_BY_GRID,
                result.status()
        );

        assertEquals(16L, result.rotatedCapacity());
    }

    @Test
    void rejectsCargoExceedingHeight() {
        var result = CargoLineSpatialFeasibility.evaluate(
                line(1200, 800, 3000, 1),
                space
        );

        assertEquals(
                CargoLineSpatialFeasibility.Status.NOT_FIT_BY_GRID,
                result.status()
        );

        assertEquals(0L, result.originalCapacity());
        assertEquals(0L, result.rotatedCapacity());
    }

    @Test
    void unknownDimensionsAreIndeterminate() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Unknown Cargo",
                null,
                null
        );

        var result = CargoLineSpatialFeasibility.evaluate(
                new CargoLine(cargo, 5),
                space
        );

        assertEquals(
                CargoLineSpatialFeasibility.Status.INDETERMINATE,
                result.status()
        );

        assertNull(result.originalCapacity());
    }

    @Test
    void supportsOpenDeckWithoutDefinedHeight() {
        var openDeck = new CargoSpaceSpecifications(
                CargoSpaceType.OPEN_DECK,
                7000,
                2400,
                null
        );

        var result = CargoLineSpatialFeasibility.evaluate(
                line(1200, 800, 3000, 10),
                openDeck
        );

        assertEquals(
                CargoLineSpatialFeasibility.Status.FITS_IN_GRID,
                result.status()
        );
    }
}
