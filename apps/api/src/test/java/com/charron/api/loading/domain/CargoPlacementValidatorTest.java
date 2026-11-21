
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.Cargo;
import com.charron.api.cargo.domain.CargoLine;
import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;
import com.charron.api.fleet.domain.CargoSpaceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CargoPlacementValidatorTest {

    private final UUID spaceId = UUID.randomUUID();

    private final CargoSpaceSpecifications space =
            new CargoSpaceSpecifications(
                    CargoSpaceType.ENCLOSED,
                    7000,
                    2400,
                    2500L
            );

    private CargoLine line(long quantity) {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Test Cargo",
                null,
                null
        );

        cargo.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(1200, 800, 1000)
                )
        );

        return new CargoLine(cargo, quantity);
    }

    private CargoPlacement placement(
            CargoLine line,
            long index,
            long x,
            long y,
            long z
    ) {
        return new CargoPlacement(
                line.cargo().getId(),
                spaceId,
                index,
                x,
                y,
                z,
                CargoSpatialFeasibility.Orientation.ORIGINAL
        );
    }

    @Test
    void acceptsGeneratedGridPlacements() {
        CargoLine line = line(16);

        var placements = CargoGridPlacementGenerator.generate(
                line, spaceId, space
        );

        var result = CargoPlacementValidator.validate(
                line, spaceId, space, placements
        );

        assertEquals(CargoPlacementValidator.Status.VALID,
                result.status());
    }

    @Test
    void rejectsPlacementOutsideBoundary() {
        CargoLine line = line(1);

        var result = CargoPlacementValidator.validate(
                line, spaceId, space,
                List.of(placement(line, 0, 6500, 0, 0))
        );

        assertEquals(CargoPlacementValidator.Status.INVALID,
                result.status());
    }

    @Test
    void rejectsOverlappingCargo() {
        CargoLine line = line(2);

        var result = CargoPlacementValidator.validate(
                line, spaceId, space,
                List.of(
                        placement(line, 0, 0, 0, 0),
                        placement(line, 1, 600, 0, 0)
                )
        );

        assertEquals(CargoPlacementValidator.Status.INVALID,
                result.status());
    }

    @Test
    void rejectsDuplicateUnitIndex() {
        CargoLine line = line(2);

        var result = CargoPlacementValidator.validate(
                line, spaceId, space,
                List.of(
                        placement(line, 0, 0, 0, 0),
                        placement(line, 0, 1200, 0, 0)
                )
        );

        assertEquals(CargoPlacementValidator.Status.INVALID,
                result.status());
    }

    @Test
    void acceptsAdjacentNonOverlappingCargo() {
        CargoLine line = line(2);

        var result = CargoPlacementValidator.validate(
                line, spaceId, space,
                List.of(
                        placement(line, 0, 0, 0, 0),
                        placement(line, 1, 1200, 0, 0)
                )
        );

        assertEquals(CargoPlacementValidator.Status.VALID,
                result.status());
    }
}
