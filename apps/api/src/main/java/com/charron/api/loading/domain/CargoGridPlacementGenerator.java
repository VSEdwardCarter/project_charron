
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.CargoLine;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CargoGridPlacementGenerator {

    private CargoGridPlacementGenerator() {
    }

    public static List<CargoPlacement> generate(
            CargoLine line,
            UUID cargoSpaceId,
            CargoSpaceSpecifications space
    ) {
        Objects.requireNonNull(line, "Cargo line is required");
        Objects.requireNonNull(
                cargoSpaceId,
                "Cargo space ID is required"
        );
        Objects.requireNonNull(space, "Cargo space is required");

        var feasibility = CargoLineSpatialFeasibility.evaluate(
                line,
                space
        );

        if (feasibility.status()
                != CargoLineSpatialFeasibility.Status.FITS_IN_GRID) {
            throw new IllegalStateException(
                    "Cannot generate placements: "
                            + feasibility.reason()
            );
        }

        Dimensions dimensions =
                line.cargo().getMeasurements().dimensions();

        var orientation = feasibility.orientation();

        long unitLength = orientation
                == CargoSpatialFeasibility.Orientation.ROTATED_90
                ? dimensions.widthMm()
                : dimensions.lengthMm();

        long unitWidth = orientation
                == CargoSpatialFeasibility.Orientation.ROTATED_90
                ? dimensions.lengthMm()
                : dimensions.widthMm();

        long columns = space.usableLengthMm() / unitLength;

        // The generator returns an in-memory Java List.
        // Prevent quantities beyond its supported index range.
        int count = Math.toIntExact(line.quantity());

        List<CargoPlacement> placements = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            long column = i % columns;
            long row = i / columns;

            long xMm = Math.multiplyExact(
                    column,
                    unitLength
            );

            long yMm = Math.multiplyExact(
                    row,
                    unitWidth
            );

            placements.add(
                    new CargoPlacement(
                            line.cargo().getId(),
                            cargoSpaceId,
                            i,
                            xMm,
                            yMm,
                            0,
                            orientation
                    )
            );
        }

        return List.copyOf(placements);
    }
}
