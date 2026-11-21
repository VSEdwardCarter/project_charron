
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;

import java.util.Objects;

public final class CargoSpatialFeasibility {

    private CargoSpatialFeasibility() {
    }

    public enum Status {
        FITS,
        DOES_NOT_FIT,
        INDETERMINATE
    }

    public enum Orientation {
        ORIGINAL,
        ROTATED_90
    }

    public record Result(
            Status status,
            Orientation orientation,
            String reason
    ) {}

    public static Result evaluate(
            CargoMeasurements measurements,
            CargoSpaceSpecifications space
    ) {
        Objects.requireNonNull(
                space,
                "Cargo space is required"
        );

        if (measurements == null
                || !measurements.hasDimensions()) {
            return new Result(
                    Status.INDETERMINATE,
                    null,
                    "Cargo dimensions are unknown"
            );
        }

        Dimensions cargo = measurements.dimensions();

        if (space.hasDefinedHeight()
                && cargo.heightMm() > space.usableHeightMm()) {
            return new Result(
                    Status.DOES_NOT_FIT,
                    null,
                    "Cargo height exceeds usable height"
            );
        }

        // Original upright orientation.
        if (cargo.lengthMm() <= space.usableLengthMm()
                && cargo.widthMm() <= space.usableWidthMm()) {
            return new Result(
                    Status.FITS,
                    Orientation.ORIGINAL,
                    "Cargo fits in original orientation"
            );
        }

        // Rotate horizontally without changing upright height.
        if (cargo.widthMm() <= space.usableLengthMm()
                && cargo.lengthMm() <= space.usableWidthMm()) {
            return new Result(
                    Status.FITS,
                    Orientation.ROTATED_90,
                    "Cargo fits after 90-degree horizontal rotation"
            );
        }

        return new Result(
                Status.DOES_NOT_FIT,
                null,
                "Cargo exceeds usable floor dimensions"
        );
    }
}
