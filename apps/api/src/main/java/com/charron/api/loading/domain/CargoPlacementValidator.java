
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.CargoLine;
import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class CargoPlacementValidator {

    private CargoPlacementValidator() {
    }

    public enum Status {
        VALID,
        INVALID,
        INDETERMINATE
    }

    public record Result(Status status, String reason) {}

    public static Result validate(
            CargoLine line,
            UUID cargoSpaceId,
            CargoSpaceSpecifications space,
            List<CargoPlacement> placements
    ) {
        Objects.requireNonNull(line, "Cargo line is required");
        Objects.requireNonNull(cargoSpaceId, "Cargo space ID is required");
        Objects.requireNonNull(space, "Cargo space is required");
        Objects.requireNonNull(placements, "Placements are required");

        CargoMeasurements measurements =
                line.cargo().getMeasurements();

        if (measurements == null || !measurements.hasDimensions()) {
            return new Result(
                    Status.INDETERMINATE,
                    "Cargo dimensions are unknown"
            );
        }

        Dimensions dimensions = measurements.dimensions();

        if (placements.size() != line.quantity()) {
            return new Result(
                    Status.INVALID,
                    "Placement count does not match cargo quantity"
            );
        }

        Set<Long> unitIndices = new HashSet<>();

        for (CargoPlacement placement : placements) {
            if (placement == null
                    || !line.cargo().getId().equals(placement.cargoId())
                    || !cargoSpaceId.equals(placement.cargoSpaceId())) {
                return new Result(
                        Status.INVALID,
                        "Placement references incorrect cargo or cargo space"
                );
            }

            if (placement.unitIndex() >= line.quantity()
                    || !unitIndices.add(placement.unitIndex())) {
                return new Result(
                        Status.INVALID,
                        "Invalid or duplicate cargo unit index"
                );
            }

            long length = placedLength(placement, dimensions);
            long width = placedWidth(placement, dimensions);

            if (length > space.usableLengthMm()
                    || placement.xMm() > space.usableLengthMm() - length
                    || width > space.usableWidthMm()
                    || placement.yMm() > space.usableWidthMm() - width) {
                return new Result(
                        Status.INVALID,
                        "Cargo placement exceeds floor boundaries"
                );
            }

            if (space.hasDefinedHeight()
                    && (dimensions.heightMm() > space.usableHeightMm()
                    || placement.zMm() >
                    space.usableHeightMm() - dimensions.heightMm())) {
                return new Result(
                        Status.INVALID,
                        "Cargo placement exceeds height boundary"
                );
            }
        }

        for (int i = 0; i < placements.size(); i++) {
            CargoPlacement first = placements.get(i);

            for (int j = i + 1; j < placements.size(); j++) {
                CargoPlacement second = placements.get(j);

                if (overlaps(first, second, dimensions)) {
                    return new Result(
                            Status.INVALID,
                            "Cargo placements overlap"
                    );
                }
            }
        }

        return new Result(
                Status.VALID,
                "All placements are within bounds and non-overlapping"
        );
    }

    private static long placedLength(
            CargoPlacement placement,
            Dimensions dimensions
    ) {
        return placement.orientation()
                == CargoSpatialFeasibility.Orientation.ROTATED_90
                ? dimensions.widthMm()
                : dimensions.lengthMm();
    }

    private static long placedWidth(
            CargoPlacement placement,
            Dimensions dimensions
    ) {
        return placement.orientation()
                == CargoSpatialFeasibility.Orientation.ROTATED_90
                ? dimensions.lengthMm()
                : dimensions.widthMm();
    }

    private static boolean overlaps(
            CargoPlacement a,
            CargoPlacement b,
            Dimensions dimensions
    ) {
        boolean overlapX =
                a.xMm() < b.xMm() + placedLength(b, dimensions)
                        && b.xMm() < a.xMm() + placedLength(a, dimensions);

        boolean overlapY =
                a.yMm() < b.yMm() + placedWidth(b, dimensions)
                        && b.yMm() < a.yMm() + placedWidth(a, dimensions);

        boolean overlapZ =
                a.zMm() < b.zMm() + dimensions.heightMm()
                        && b.zMm() < a.zMm() + dimensions.heightMm();

        return overlapX && overlapY && overlapZ;
    }
}
