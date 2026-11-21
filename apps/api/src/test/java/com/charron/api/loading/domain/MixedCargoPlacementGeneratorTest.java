
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

class MixedCargoPlacementGeneratorTest {

    private LoadCargoLine line(
            String name,
            long length,
            long width,
            long height,
            long quantity
    ) {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                name,
                null,
                null
        );

        cargo.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(length, width, height)
                )
        );

        return new LoadCargoLine(
                UUID.randomUUID(),
                new CargoLine(cargo, quantity)
        );
    }

    @Test
    void generatesValidPlacementsForMixedCargo() {
        UUID spaceId = UUID.randomUUID();

        CargoSpaceSpecifications space =
                new CargoSpaceSpecifications(
                        CargoSpaceType.ENCLOSED,
                        7000,
                        2400,
                        2500L
                );

        LoadCargoLine generators = line(
                "Generator",
                1200, 800, 1000,
                3
        );

        LoadCargoLine crates = line(
                "Supply Crate",
                600, 600, 600,
                4
        );

        List<LoadCargoLine> lines =
                List.of(generators, crates);

        var placements =
                MixedCargoPlacementGenerator.generate(
                        lines,
                        spaceId,
                        space
                );

        assertEquals(7, placements.size());

        var validation = LoadPlanValidator.validate(
                lines,
                placements,
                Map.of(spaceId, space)
        );

        assertEquals(
                LoadPlanValidator.Status.VALID,
                validation.status(),
                validation.reason()
        );

        for (var placement : placements) {
            assertEquals(
                    spaceId,
                    placement.placement().cargoSpaceId()
            );

            assertEquals(
                    0,
                    placement.placement().zMm()
            );
        }
    }

    @Test
    void startsNewRowWhenCurrentRowIsFull() {
        UUID spaceId = UUID.randomUUID();

        CargoSpaceSpecifications space =
                new CargoSpaceSpecifications(
                        CargoSpaceType.ENCLOSED,
                        3000,
                        2000,
                        2500L
                );

        LoadCargoLine cargoLine = line(
                "Equipment Crate",
                1200, 800, 1000,
                3
        );

        var placements = MixedCargoPlacementGenerator.generate(
                List.of(cargoLine),
                spaceId,
                space
        );

        assertEquals(3, placements.size());

        // Two units fit along the first row.
        assertEquals(0, placements.get(0).placement().xMm());
        assertEquals(0, placements.get(0).placement().yMm());

        assertEquals(1200, placements.get(1).placement().xMm());
        assertEquals(0, placements.get(1).placement().yMm());

        // The third unit starts a new row.
        assertEquals(0, placements.get(2).placement().xMm());
        assertEquals(800, placements.get(2).placement().yMm());

        var validation = LoadPlanValidator.validate(
                List.of(cargoLine),
                placements,
                Map.of(spaceId, space)
        );

        assertEquals(
                LoadPlanValidator.Status.VALID,
                validation.status(),
                validation.reason()
        );
    }


    @Test
    void rejectsCargoThatCannotFitWithinAvailableFloorSpace() {
        UUID spaceId = UUID.randomUUID();

        CargoSpaceSpecifications space =
                new CargoSpaceSpecifications(
                        CargoSpaceType.ENCLOSED,
                        3000,
                        2000,
                        2500L
                );

        LoadCargoLine oversized = line(
                "Oversized Equipment",
                4000, 2500, 1000,
                1
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> MixedCargoPlacementGenerator.generate(
                        List.of(oversized),
                        spaceId,
                        space
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "Unable to place all cargo units"
                )
        );
    }

    @Test
    void rejectsCargoWithUnknownDimensions() {
        UUID spaceId = UUID.randomUUID();

        CargoSpaceSpecifications space =
                new CargoSpaceSpecifications(
                        CargoSpaceType.ENCLOSED,
                        7000,
                        2400,
                        2500L
                );

        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Unmeasured Equipment",
                null,
                null
        );

        LoadCargoLine unmeasured = new LoadCargoLine(
                UUID.randomUUID(),
                new CargoLine(cargo, 1)
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> MixedCargoPlacementGenerator.generate(
                        List.of(unmeasured),
                        spaceId,
                        space
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "Cargo dimensions are unknown"
                )
        );
    }

}
