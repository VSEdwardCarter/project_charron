
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;
import com.charron.api.fleet.domain.CargoSpaceType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CargoSpatialFeasibilityTest {

    private final CargoSpaceSpecifications enclosed =
            new CargoSpaceSpecifications(
                    CargoSpaceType.ENCLOSED,
                    7000,
                    2400,
                    2500L
            );

    private CargoMeasurements cargo(
            long length,
            long width,
            long height
    ) {
        return new CargoMeasurements(
                null,
                new Dimensions(length, width, height)
        );
    }

    @Test
    void fitsInOriginalOrientation() {
        var result = CargoSpatialFeasibility.evaluate(
                cargo(1200, 800, 1000),
                enclosed
        );

        assertEquals(
                CargoSpatialFeasibility.Status.FITS,
                result.status()
        );

        assertEquals(
                CargoSpatialFeasibility.Orientation.ORIGINAL,
                result.orientation()
        );
    }

    @Test
    void fitsAfterHorizontalRotation() {
        var result = CargoSpatialFeasibility.evaluate(
                cargo(2000, 6000, 1000),
                enclosed
        );

        assertEquals(
                CargoSpatialFeasibility.Status.FITS,
                result.status()
        );

        assertEquals(
                CargoSpatialFeasibility.Orientation.ROTATED_90,
                result.orientation()
        );
    }

    @Test
    void rejectsOversizedCargo() {
        var result = CargoSpatialFeasibility.evaluate(
                cargo(8000, 3000, 1000),
                enclosed
        );

        assertEquals(
                CargoSpatialFeasibility.Status.DOES_NOT_FIT,
                result.status()
        );
    }

    @Test
    void rejectsExcessiveHeight() {
        var result = CargoSpatialFeasibility.evaluate(
                cargo(1200, 800, 3000),
                enclosed
        );

        assertEquals(
                CargoSpatialFeasibility.Status.DOES_NOT_FIT,
                result.status()
        );
    }

    @Test
    void supportsOpenDeckWithoutHeightLimit() {
        var openDeck = new CargoSpaceSpecifications(
                CargoSpaceType.OPEN_DECK,
                7000,
                2400,
                null
        );

        var result = CargoSpatialFeasibility.evaluate(
                cargo(1200, 800, 3000),
                openDeck
        );

        assertEquals(
                CargoSpatialFeasibility.Status.FITS,
                result.status()
        );
    }

    @Test
    void missingCargoDimensionsAreIndeterminate() {
        var result = CargoSpatialFeasibility.evaluate(
                null,
                enclosed
        );

        assertEquals(
                CargoSpatialFeasibility.Status.INDETERMINATE,
                result.status()
        );
    }

    @Test
    void exactDimensionsFit() {
        var result = CargoSpatialFeasibility.evaluate(
                cargo(7000, 2400, 2500),
                enclosed
        );

        assertEquals(
                CargoSpatialFeasibility.Status.FITS,
                result.status()
        );
    }
}
