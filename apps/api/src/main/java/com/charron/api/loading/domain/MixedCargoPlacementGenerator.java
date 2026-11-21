
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class MixedCargoPlacementGenerator {

    private MixedCargoPlacementGenerator() {
    }

    public static List<LoadCargoPlacement> generate(
            List<LoadCargoLine> lines,
            UUID cargoSpaceId,
            CargoSpaceSpecifications space
    ) {
        Objects.requireNonNull(lines, "Cargo lines are required");
        Objects.requireNonNull(cargoSpaceId, "Cargo space ID is required");
        Objects.requireNonNull(space, "Cargo space is required");

        long x = 0;
        long y = 0;
        long rowWidth = 0;

        List<LoadCargoPlacement> placements = new ArrayList<>();

        for (LoadCargoLine line : lines) {
            Objects.requireNonNull(line, "Cargo line cannot be null");

            CargoMeasurements measurements =
                    line.cargoLine().cargo().getMeasurements();

            if (measurements == null
                    || !measurements.hasDimensions()) {
                throw new IllegalStateException(
                        "Cargo dimensions are unknown for line " + line.id()
                );
            }

            Dimensions dimensions = measurements.dimensions();

            if (space.hasDefinedHeight()
                    && dimensions.heightMm() > space.usableHeightMm()) {
                throw new IllegalStateException(
                        "Cargo exceeds usable height"
                );
            }

            // An in-memory list cannot represent more
            // than Integer.MAX_VALUE placements.
            if (line.quantity() > Integer.MAX_VALUE - placements.size()) {
                throw new IllegalArgumentException(
                        "Too many cargo units for an in-memory plan"
                );
            }

            for (long unit = 0; unit < line.quantity(); unit++) {
                OrientationChoice choice = chooseOrientation(
                        dimensions,
                        space,
                        x,
                        y
                );

                if (choice == null) {
                    // Start a new row.
                    y = Math.addExact(y, rowWidth);
                    x = 0;
                    rowWidth = 0;

                    choice = chooseOrientation(
                            dimensions,
                            space,
                            x,
                            y
                    );
                }

                if (choice == null) {
                    throw new IllegalStateException(
                            "Unable to place all cargo units "
                                    + "using the row placement strategy"
                    );
                }

                CargoPlacement placement = new CargoPlacement(
                        line.cargoId(),
                        cargoSpaceId,
                        unit,
                        x,
                        y,
                        0,
                        choice.orientation()
                );

                placements.add(
                        new LoadCargoPlacement(
                                line.id(),
                                placement
                        )
                );

                x = Math.addExact(x, choice.lengthMm());

                rowWidth = Math.max(
                        rowWidth,
                        choice.widthMm()
                );
            }
        }

        return List.copyOf(placements);
    }

    private static OrientationChoice chooseOrientation(
            Dimensions cargo,
            CargoSpaceSpecifications space,
            long x,
            long y
    ) {
        if (fits(
                cargo.lengthMm(),
                cargo.widthMm(),
                space,
                x,
                y
        )) {
            return new OrientationChoice(
                    cargo.lengthMm(),
                    cargo.widthMm(),
                    CargoSpatialFeasibility.Orientation.ORIGINAL
            );
        }

        if (fits(
                cargo.widthMm(),
                cargo.lengthMm(),
                space,
                x,
                y
        )) {
            return new OrientationChoice(
                    cargo.widthMm(),
                    cargo.lengthMm(),
                    CargoSpatialFeasibility.Orientation.ROTATED_90
            );
        }

        return null;
    }

    private static boolean fits(
            long length,
            long width,
            CargoSpaceSpecifications space,
            long x,
            long y
    ) {
        return length <= space.usableLengthMm()
                && x <= space.usableLengthMm() - length
                && width <= space.usableWidthMm()
                && y <= space.usableWidthMm() - width;
    }

    private record OrientationChoice(
            long lengthMm,
            long widthMm,
            CargoSpatialFeasibility.Orientation orientation
    ) {
    }


}
