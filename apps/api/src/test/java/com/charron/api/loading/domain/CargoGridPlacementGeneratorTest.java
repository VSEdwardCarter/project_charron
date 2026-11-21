
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

class CargoGridPlacementGeneratorTest {

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
    void generatesRotatedOrientationCoordinates(){
        UUID spaceId = UUID.randomUUID();

        var placements = CargoGridPlacementGenerator.generate(
                line(1200, 800, 1000, 3),
                spaceId,
                space
        );

        assertEquals(3, placements.size());


        assertEquals(0, placements.get(0).xMm());
        assertEquals(800, placements.get(1).xMm());
        assertEquals(1600, placements.get(2).xMm());

        for (var placement : placements) {
            assertEquals(0, placement.yMm());
            assertEquals(0, placement.zMm());
            assertEquals(spaceId, placement.cargoSpaceId());
            assertEquals(
                    CargoSpatialFeasibility.Orientation.ROTATED_90,
                    placement.orientation()
            );
        }
    }

    @Test
    void generatesMultipleRowsWithoutOverlap() {
        var placements = CargoGridPlacementGenerator.generate(
                line(1200, 800, 1000, 16),
                UUID.randomUUID(),
                space
        );

        assertEquals(16, placements.size());

        // Rotated unit footprint: 800 × 1200 mm.
        // Eight columns per row.
        assertEquals(0, placements.get(0).xMm());
        assertEquals(0, placements.get(0).yMm());

        assertEquals(5600, placements.get(7).xMm());
        assertEquals(0, placements.get(7).yMm());

        assertEquals(0, placements.get(8).xMm());
        assertEquals(1200, placements.get(8).yMm());

        assertEquals(5600, placements.get(15).xMm());
        assertEquals(1200, placements.get(15).yMm());
    }

    @Test
    void rejectsQuantityExceedingGridCapacity() {
        assertThrows(
                IllegalStateException.class,
                () -> CargoGridPlacementGenerator.generate(
                        line(1200, 800, 1000, 17),
                        UUID.randomUUID(),
                        space
                )
        );
    }

    @Test
    void rejectsCargoWithUnknownDimensions() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Unknown Cargo",
                null,
                null
        );

        assertThrows(
                IllegalStateException.class,
                () -> CargoGridPlacementGenerator.generate(
                        new CargoLine(cargo, 2),
                        UUID.randomUUID(),
                        space
                )
        );
    }

    @Test
    void generatedPlacementsStayWithinCargoSpace() {
        CargoLine cargoLine = line(1200, 800, 1000, 16);

        var placements = CargoGridPlacementGenerator.generate(
                cargoLine,
                UUID.randomUUID(),
                space
        );

        assertEquals(16, placements.size());

        for (var placement : placements) {
            long placedLength =
                    placement.orientation()
                            == CargoSpatialFeasibility.Orientation.ROTATED_90
                            ? 800
                            : 1200;

            long placedWidth =
                    placement.orientation()
                            == CargoSpatialFeasibility.Orientation.ROTATED_90
                            ? 1200
                            : 800;

            assertTrue(
                    placement.xMm() + placedLength
                            <= space.usableLengthMm()
            );

            assertTrue(
                    placement.yMm() + placedWidth
                            <= space.usableWidthMm()
            );

            assertTrue(
                    placement.zMm() + 1000
                            <= space.usableHeightMm()
            );
        }
    }


    @Test
    void generatedPlacementsDoNotOverlap() {
        var placements = CargoGridPlacementGenerator.generate(
                line(1200, 800, 1000, 16),
                UUID.randomUUID(),
                space
        );

        // The generator chooses rotated orientation:
        // 800 mm length x 1200 mm width.
        long placedLength = 800;
        long placedWidth = 1200;

        for (int i = 0; i < placements.size(); i++) {
            var first = placements.get(i);

            for (int j = i + 1; j < placements.size(); j++) {
                var second = placements.get(j);

                boolean overlapsX =
                        first.xMm() < second.xMm() + placedLength
                                && second.xMm() < first.xMm() + placedLength;

                boolean overlapsY =
                        first.yMm() < second.yMm() + placedWidth
                                && second.yMm() < first.yMm() + placedWidth;

                assertFalse(
                        overlapsX && overlapsY,
                        "Cargo units " + i + " and " + j
                                + " overlap"
                );
            }
        }
    }

}
