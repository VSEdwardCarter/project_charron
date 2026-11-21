
package com.charron.api.loading.domain;

import java.util.Objects;
import java.util.UUID;

public record CargoPlacement(
        UUID cargoId,
        UUID cargoSpaceId,
        long unitIndex,
        long xMm,
        long yMm,
        long zMm,
        CargoSpatialFeasibility.Orientation orientation
) {

    public CargoPlacement {
        Objects.requireNonNull(cargoId, "Cargo ID is required");
        Objects.requireNonNull(
                cargoSpaceId,
                "Cargo space ID is required"
        );
        Objects.requireNonNull(
                orientation,
                "Orientation is required"
        );

        if (unitIndex < 0) {
            throw new IllegalArgumentException(
                    "Unit index cannot be negative"
            );
        }

        if (xMm < 0 || yMm < 0 || zMm < 0) {
            throw new IllegalArgumentException(
                    "Placement coordinates cannot be negative"
            );
        }
    }
}
