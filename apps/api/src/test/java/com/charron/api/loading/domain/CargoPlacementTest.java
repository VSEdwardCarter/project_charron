
package com.charron.api.loading.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CargoPlacementTest {

    @Test
    void storesPlacementCoordinates() {
        UUID cargoId = UUID.randomUUID();
        UUID cargoSpaceId = UUID.randomUUID();

        CargoPlacement placement = new CargoPlacement(
                cargoId,
                cargoSpaceId,
                0,
                1200,
                800,
                0,
                CargoSpatialFeasibility.Orientation.ORIGINAL
        );

        assertEquals(cargoId, placement.cargoId());
        assertEquals(cargoSpaceId, placement.cargoSpaceId());
        assertEquals(0, placement.unitIndex());
        assertEquals(1200, placement.xMm());
        assertEquals(800, placement.yMm());
        assertEquals(0, placement.zMm());
    }

    @Test
    void supportsRotatedCargo() {
        CargoPlacement placement = new CargoPlacement(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                800,
                1200,
                0,
                CargoSpatialFeasibility.Orientation.ROTATED_90
        );

        assertEquals(
                CargoSpatialFeasibility.Orientation.ROTATED_90,
                placement.orientation()
        );
    }

    @Test
    void rejectsNegativeCoordinates() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CargoPlacement(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        0,
                        -1,
                        0,
                        0,
                        CargoSpatialFeasibility.Orientation.ORIGINAL
                )
        );
    }

    @Test
    void rejectsNegativeUnitIndex() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CargoPlacement(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        -1,
                        0,
                        0,
                        0,
                        CargoSpatialFeasibility.Orientation.ORIGINAL
                )
        );
    }

    @Test
    void rejectsMissingIdentityOrOrientation() {
        UUID id = UUID.randomUUID();

        assertThrows(
                NullPointerException.class,
                () -> new CargoPlacement(
                        null, id, 0, 0, 0, 0,
                        CargoSpatialFeasibility.Orientation.ORIGINAL
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new CargoPlacement(
                        id, null, 0, 0, 0, 0,
                        CargoSpatialFeasibility.Orientation.ORIGINAL
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new CargoPlacement(
                        id, id, 0, 0, 0, 0, null
                )
        );
    }
}
