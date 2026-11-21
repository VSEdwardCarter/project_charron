
package com.charron.api.fleet.domain;

import com.charron.api.common.measurement.Dimensions;

import java.util.Objects;

public record CargoSpaceSpecifications(
        CargoSpaceType type,
        long usableLengthMm,
        long usableWidthMm,
        Long usableHeightMm
) {

    public CargoSpaceSpecifications {
        Objects.requireNonNull(
                type,
                "Cargo space type is required"
        );

        if (usableLengthMm <= 0) {
            throw new IllegalArgumentException(
                    "Usable length must be greater than zero"
            );
        }

        if (usableWidthMm <= 0) {
            throw new IllegalArgumentException(
                    "Usable width must be greater than zero"
            );
        }

        if (usableHeightMm != null && usableHeightMm <= 0) {
            throw new IllegalArgumentException(
                    "Usable height must be greater than zero"
            );
        }

        if (type == CargoSpaceType.ENCLOSED
                && usableHeightMm == null) {
            throw new IllegalArgumentException(
                    "Enclosed cargo spaces require usable height"
            );
        }
    }

    public boolean hasDefinedHeight() {
        return usableHeightMm != null;
    }

    public Dimensions usableDimensions() {
        if (usableHeightMm == null) {
            throw new IllegalStateException(
                    "Cargo space does not have a defined usable height"
            );
        }

        return new Dimensions(
                usableLengthMm,
                usableWidthMm,
                usableHeightMm
        );
    }
}
