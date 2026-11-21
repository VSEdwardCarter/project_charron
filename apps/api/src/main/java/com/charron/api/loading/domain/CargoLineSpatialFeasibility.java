
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.CargoLine;
import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;

import java.util.Objects;

public final class CargoLineSpatialFeasibility {

    private CargoLineSpatialFeasibility() {
    }

    public enum Status {
        FITS_IN_GRID,
        NOT_FIT_BY_GRID,
        INDETERMINATE
    }

    public record Result(
            Status status,
            long requestedQuantity,
            Long originalCapacity,
            Long rotatedCapacity,
            CargoSpatialFeasibility.Orientation orientation,
            String reason
    ) {}

    public static Result evaluate(
            CargoLine line,
            CargoSpaceSpecifications space
    ) {
        Objects.requireNonNull(line, "Cargo line is required");
        Objects.requireNonNull(space, "Cargo space is required");

        CargoMeasurements measurements =
                line.cargo().getMeasurements();

        if (measurements == null
                || !measurements.hasDimensions()) {
            return new Result(
                    Status.INDETERMINATE,
                    line.quantity(),
                    null,
                    null,
                    null,
                    "Cargo dimensions are unknown"
            );
        }

        Dimensions cargo = measurements.dimensions();

        if (space.hasDefinedHeight()
                && cargo.heightMm() > space.usableHeightMm()) {
            return new Result(
                    Status.NOT_FIT_BY_GRID,
                    line.quantity(),
                    0L,
                    0L,
                    null,
                    "Cargo exceeds usable height"
            );
        }

        long originalCapacity = gridCapacity(
                space.usableLengthMm(),
                space.usableWidthMm(),
                cargo.lengthMm(),
                cargo.widthMm()
        );

        long rotatedCapacity = gridCapacity(
                space.usableLengthMm(),
                space.usableWidthMm(),
                cargo.widthMm(),
                cargo.lengthMm()
        );

        boolean rotate = rotatedCapacity > originalCapacity;

        long bestCapacity = rotate
                ? rotatedCapacity
                : originalCapacity;

        var orientation = rotate
                ? CargoSpatialFeasibility.Orientation.ROTATED_90
                : CargoSpatialFeasibility.Orientation.ORIGINAL;

        if (bestCapacity < line.quantity()) {
            return new Result(
                    Status.NOT_FIT_BY_GRID,
                    line.quantity(),
                    originalCapacity,
                    rotatedCapacity,
                    null,
                    "Requested quantity exceeds uniform grid capacity"
            );
        }

        return new Result(
                Status.FITS_IN_GRID,
                line.quantity(),
                originalCapacity,
                rotatedCapacity,
                orientation,
                "Requested quantity fits in a uniform floor grid"
        );
    }

    private static long gridCapacity(
            long spaceLength,
            long spaceWidth,
            long cargoLength,
            long cargoWidth
    ) {
        long columns = spaceLength / cargoLength;
        long rows = spaceWidth / cargoWidth;

        // Saturate if theoretical capacity exceeds long range.
        if (rows > 0 && columns > Long.MAX_VALUE / rows) {
            return Long.MAX_VALUE;
        }

        return columns * rows;
    }
}
