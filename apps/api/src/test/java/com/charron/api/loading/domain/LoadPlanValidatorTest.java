
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.Cargo;
import com.charron.api.cargo.domain.CargoLine;
import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;
import com.charron.api.fleet.domain.CargoSpaceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoadPlanValidatorTest {

    private final UUID spaceId = UUID.randomUUID();

    private LoadCargoLine line(Cargo cargo, long quantity) {
        return new LoadCargoLine(
                UUID.randomUUID(),
                new CargoLine(cargo, quantity)
        );
    }

    private LoadCargoPlacement placement(
            LoadCargoLine line,
            long unitIndex
    ) {
        return new LoadCargoPlacement(
                line.id(),
                new CargoPlacement(
                        line.cargoId(),
                        spaceId,
                        unitIndex,
                        0,
                        0,
                        0,
                        CargoSpatialFeasibility.Orientation.ORIGINAL
                )
        );
    }

    @Test
    void acceptsMultipleLinesReferencingSameCargo() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Generator",
                null,
                null
        );

        LoadCargoLine first = line(cargo, 2);
        LoadCargoLine second = line(cargo, 1);

        var result = LoadPlanValidator.validateOwnership(
                List.of(first, second),
                List.of(
                        placement(first, 0),
                        placement(first, 1),
                        placement(second, 0)
                )
        );

        assertEquals(
                LoadPlanValidator.Status.VALID,
                result.status()
        );
    }

    @Test
    void rejectsUnknownCargoLine() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Generator",
                null,
                null
        );

        LoadCargoLine registered = line(cargo, 1);
        LoadCargoLine unknown = line(cargo, 1);

        var result = LoadPlanValidator.validateOwnership(
                List.of(registered),
                List.of(placement(unknown, 0))
        );

        assertEquals(
                LoadPlanValidator.Status.INVALID,
                result.status()
        );
    }

    @Test
    void rejectsDuplicateUnitWithinSameLine() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Generator",
                null,
                null
        );

        LoadCargoLine line = line(cargo, 2);

        var result = LoadPlanValidator.validateOwnership(
                List.of(line),
                List.of(
                        placement(line, 0),
                        placement(line, 0)
                )
        );

        assertEquals(
                LoadPlanValidator.Status.INVALID,
                result.status()
        );
    }

    @Test
    void rejectsMissingCargoUnits() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Generator",
                null,
                null
        );

        LoadCargoLine line = line(cargo, 3);

        var result = LoadPlanValidator.validateOwnership(
                List.of(line),
                List.of(
                        placement(line, 0),
                        placement(line, 1)
                )
        );

        assertEquals(
                LoadPlanValidator.Status.INVALID,
                result.status()
        );
    }

    @Test
    void rejectsPlacementWithIncorrectCargoIdentity() {
        Cargo expectedCargo = new Cargo(
                UUID.randomUUID(), "Generator", null, null
        );

        Cargo otherCargo = new Cargo(
                UUID.randomUUID(), "Pallet", null, null
        );

        LoadCargoLine line = line(expectedCargo, 1);

        var incorrect = new LoadCargoPlacement(
                line.id(),
                new CargoPlacement(
                        otherCargo.getId(),
                        spaceId,
                        0,
                        0,
                        0,
                        0,
                        CargoSpatialFeasibility.Orientation.ORIGINAL
                )
        );

        var result = LoadPlanValidator.validateOwnership(
                List.of(line),
                List.of(incorrect)
        );

        assertEquals(
                LoadPlanValidator.Status.INVALID,
                result.status()
        );
    }


    @Test
    void rejectsOverlappingPlacementsAcrossCargoLines() {
        Cargo generator = new Cargo(
                UUID.randomUUID(),
                "Generator",
                null,
                null
        );

        generator.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(1200, 800, 1000)
                )
        );

        Cargo crate = new Cargo(
                UUID.randomUUID(),
                "Supply Crate",
                null,
                null
        );

        crate.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(600, 600, 600)
                )
        );

        LoadCargoLine generatorLine = line(generator, 1);
        LoadCargoLine crateLine = line(crate, 1);

        var result = LoadPlanValidator.validateCollisions(
                List.of(generatorLine, crateLine),
                List.of(
                        placement(generatorLine, 0),
                        placement(crateLine, 0)
                )
        );

        assertEquals(
                LoadPlanValidator.Status.INVALID,
                result.status()
        );

        assertTrue(result.reason().contains("overlap"));
    }


    @Test
    void acceptsAdjacentCargoAcrossDifferentLines() {
        Cargo generator = new Cargo(
                UUID.randomUUID(), "Generator", null, null
        );
        generator.setMeasurements(
                new CargoMeasurements(
                        null, new Dimensions(1200, 800, 1000)
                )
        );

        Cargo crate = new Cargo(
                UUID.randomUUID(), "Crate", null, null
        );
        crate.setMeasurements(
                new CargoMeasurements(
                        null, new Dimensions(600, 600, 600)
                )
        );

        LoadCargoLine first = line(generator, 1);
        LoadCargoLine second = line(crate, 1);

        var adjacent = new LoadCargoPlacement(
                second.id(),
                new CargoPlacement(
                        second.cargoId(),
                        spaceId,
                        0,
                        1200, 0, 0,
                        CargoSpatialFeasibility.Orientation.ORIGINAL
                )
        );

        var result = LoadPlanValidator.validateCollisions(
                List.of(first, second),
                List.of(
                        placement(first, 0),
                        adjacent
                )
        );

        assertEquals(
                LoadPlanValidator.Status.VALID,
                result.status()
        );
    }

    @Test
    void acceptsOverlappingCoordinatesInSeparateCargoSpaces() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(), "Generator", null, null
        );
        cargo.setMeasurements(
                new CargoMeasurements(
                        null, new Dimensions(1200, 800, 1000)
                )
        );

        LoadCargoLine first = line(cargo, 1);
        LoadCargoLine second = line(cargo, 1);

        var otherSpacePlacement = new LoadCargoPlacement(
                second.id(),
                new CargoPlacement(
                        second.cargoId(),
                        UUID.randomUUID(),
                        0,
                        0, 0, 0,
                        CargoSpatialFeasibility.Orientation.ORIGINAL
                )
        );

        var result = LoadPlanValidator.validateCollisions(
                List.of(first, second),
                List.of(
                        placement(first, 0),
                        otherSpacePlacement
                )
        );

        assertEquals(
                LoadPlanValidator.Status.VALID,
                result.status()
        );
    }

    @Test
    void rejectsCollisionEvaluationWhenDimensionsUnknown() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(), "Unmeasured Freight", null, null
        );

        LoadCargoLine unmeasured = line(cargo, 1);

        var result = LoadPlanValidator.validateCollisions(
                List.of(unmeasured),
                List.of(placement(unmeasured, 0))
        );

        assertEquals(
                LoadPlanValidator.Status.INVALID,
                result.status()
        );

        assertTrue(
                result.reason().contains("dimensions are unknown")
        );
    }


    @Test
    void acceptsCargoWithinAssignedSpace() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(), "Generator", null, null
        );

        cargo.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(1200, 800, 1000)
                )
        );

        LoadCargoLine cargoLine = line(cargo, 1);

        var space = new CargoSpaceSpecifications(
                CargoSpaceType.ENCLOSED,
                7000,
                2400,
                2500L
        );

        var result = LoadPlanValidator.validateBoundaries(
                List.of(cargoLine),
                List.of(placement(cargoLine, 0)),
                Map.of(spaceId, space)
        );

        assertEquals(
                LoadPlanValidator.Status.VALID,
                result.status()
        );
    }

    @Test
    void rejectsCargoOutsideAssignedSpace() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(), "Generator", null, null
        );

        cargo.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(1200, 800, 1000)
                )
        );

        LoadCargoLine cargoLine = line(cargo, 1);

        var space = new CargoSpaceSpecifications(
                CargoSpaceType.ENCLOSED,
                7000,
                2400,
                2500L
        );

        var outside = new LoadCargoPlacement(
                cargoLine.id(),
                new CargoPlacement(
                        cargoLine.cargoId(),
                        spaceId,
                        0,
                        6500, 0, 0,
                        CargoSpatialFeasibility.Orientation.ORIGINAL
                )
        );

        var result = LoadPlanValidator.validateBoundaries(
                List.of(cargoLine),
                List.of(outside),
                Map.of(spaceId, space)
        );

        assertEquals(
                LoadPlanValidator.Status.INVALID,
                result.status()
        );

        assertTrue(
                result.reason().contains("length boundary")
        );
    }

    @Test
    void rejectsUnknownCargoSpace() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(), "Generator", null, null
        );

        cargo.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(1200, 800, 1000)
                )
        );

        LoadCargoLine cargoLine = line(cargo, 1);

        var result = LoadPlanValidator.validateBoundaries(
                List.of(cargoLine),
                List.of(placement(cargoLine, 0)),
                Map.of()
        );

        assertEquals(
                LoadPlanValidator.Status.INVALID,
                result.status()
        );

        assertTrue(
                result.reason().contains("unknown cargo space")
        );
    }

    @Test
    void unifiedValidationAcceptsValidMixedCargo() {
        Cargo generator = new Cargo(
                UUID.randomUUID(), "Generator", null, null
        );
        generator.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(1200, 800, 1000)
                )
        );

        Cargo crate = new Cargo(
                UUID.randomUUID(), "Supply Crate", null, null
        );
        crate.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(600, 600, 600)
                )
        );

        LoadCargoLine first = line(generator, 1);
        LoadCargoLine second = line(crate, 1);

        var cratePlacement = new LoadCargoPlacement(
                second.id(),
                new CargoPlacement(
                        second.cargoId(),
                        spaceId,
                        0,
                        1200, 0, 0,
                        CargoSpatialFeasibility.Orientation.ORIGINAL
                )
        );

        var space = new CargoSpaceSpecifications(
                CargoSpaceType.ENCLOSED,
                7000, 2400, 2500L
        );

        var result = LoadPlanValidator.validate(
                List.of(first, second),
                List.of(
                        placement(first, 0),
                        cratePlacement
                ),
                Map.of(spaceId, space)
        );

        assertEquals(
                LoadPlanValidator.Status.VALID,
                result.status()
        );
    }

    @Test
    void unifiedValidationRejectsCrossLineCollision() {
        Cargo generator = new Cargo(
                UUID.randomUUID(), "Generator", null, null
        );
        generator.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(1200, 800, 1000)
                )
        );

        Cargo crate = new Cargo(
                UUID.randomUUID(), "Crate", null, null
        );
        crate.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(600, 600, 600)
                )
        );

        LoadCargoLine first = line(generator, 1);
        LoadCargoLine second = line(crate, 1);

        var space = new CargoSpaceSpecifications(
                CargoSpaceType.ENCLOSED,
                7000, 2400, 2500L
        );

        var result = LoadPlanValidator.validate(
                List.of(first, second),
                List.of(
                        placement(first, 0),
                        placement(second, 0)
                ),
                Map.of(spaceId, space)
        );

        assertEquals(
                LoadPlanValidator.Status.INVALID,
                result.status()
        );

        assertTrue(result.reason().contains("overlap"));
    }

    @Test
    void unifiedValidationRejectsBoundaryViolation() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(), "Generator", null, null
        );
        cargo.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(1200, 800, 1000)
                )
        );

        LoadCargoLine cargoLine = line(cargo, 1);

        var outside = new LoadCargoPlacement(
                cargoLine.id(),
                new CargoPlacement(
                        cargoLine.cargoId(),
                        spaceId,
                        0,
                        6500, 0, 0,
                        CargoSpatialFeasibility.Orientation.ORIGINAL
                )
        );

        var space = new CargoSpaceSpecifications(
                CargoSpaceType.ENCLOSED,
                7000, 2400, 2500L
        );

        var result = LoadPlanValidator.validate(
                List.of(cargoLine),
                List.of(outside),
                Map.of(spaceId, space)
        );

        assertEquals(
                LoadPlanValidator.Status.INVALID,
                result.status()
        );

        assertTrue(
                result.reason().contains("length boundary")
        );
    }


}
