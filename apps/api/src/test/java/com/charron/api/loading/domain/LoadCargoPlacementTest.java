
package com.charron.api.loading.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoadCargoPlacementTest {

    private CargoPlacement placement(
            UUID cargoId,
            long unitIndex
    ) {
        return new CargoPlacement(
                cargoId,
                UUID.randomUUID(),
                unitIndex,
                0,
                0,
                0,
                CargoSpatialFeasibility.Orientation.ORIGINAL
        );
    }

    @Test
    void associatesPlacementWithLoadCargoLine() {
        UUID lineId = UUID.randomUUID();
        UUID cargoId = UUID.randomUUID();

        LoadCargoPlacement result = new LoadCargoPlacement(
                lineId,
                placement(cargoId, 0)
        );

        assertEquals(lineId, result.loadCargoLineId());
        assertEquals(cargoId, result.cargoId());
        assertEquals(0, result.unitIndex());
    }

    @Test
    void distinguishesLinesReferencingSameCargo() {
        UUID cargoId = UUID.randomUUID();

        LoadCargoPlacement first = new LoadCargoPlacement(
                UUID.randomUUID(),
                placement(cargoId, 0)
        );

        LoadCargoPlacement second = new LoadCargoPlacement(
                UUID.randomUUID(),
                placement(cargoId, 0)
        );

        assertEquals(first.cargoId(), second.cargoId());
        assertEquals(first.unitIndex(), second.unitIndex());

        assertNotEquals(
                first.loadCargoLineId(),
                second.loadCargoLineId()
        );
    }

    @Test
    void rejectsMissingLoadCargoLineIdentity() {
        assertThrows(
                NullPointerException.class,
                () -> new LoadCargoPlacement(
                        null,
                        placement(UUID.randomUUID(), 0)
                )
        );
    }

    @Test
    void rejectsMissingPlacement() {
        assertThrows(
                NullPointerException.class,
                () -> new LoadCargoPlacement(
                        UUID.randomUUID(),
                        null
                )
        );
    }
}
