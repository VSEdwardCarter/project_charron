
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class LoadPlanValidator {

    private LoadPlanValidator() {
    }

    public enum Status {
        VALID,
        INVALID,
        INDETERMINATE
    }

    public record Result(
            Status status,
            String reason
    ) {
    }

    public static Result validate(
            List<LoadCargoLine> lines,
            List<LoadCargoPlacement> placements,
            Map<UUID, CargoSpaceSpecifications> cargoSpaces
    ) {
        Objects.requireNonNull(
                cargoSpaces,
                "Cargo spaces are required"
        );

        Result ownership = validateOwnership(
                lines,
                placements
        );

        if (ownership.status() != Status.VALID) {
            return ownership;
        }

        Result boundaries = validateBoundaries(
                lines,
                placements,
                cargoSpaces
        );

        if (boundaries.status() != Status.VALID) {
            return boundaries;
        }

        Result collisions = validateCollisions(
                lines,
                placements
        );

        if (collisions.status() != Status.VALID) {
            return collisions;
        }

        return valid(
                "Loading plan passed ownership, boundary, "
                        + "and collision validation"
        );
    }

    public static Result validateOwnership(
            List<LoadCargoLine> lines,
            List<LoadCargoPlacement> placements
    ) {
        Objects.requireNonNull(
                lines,
                "Cargo lines are required"
        );

        Objects.requireNonNull(
                placements,
                "Cargo placements are required"
        );

        Map<UUID, LoadCargoLine> linesById =
                new HashMap<>();

        for (LoadCargoLine line : lines) {
            if (line == null) {
                return invalid(
                        "Cargo line cannot be null"
                );
            }

            if (linesById.putIfAbsent(
                    line.id(),
                    line
            ) != null) {
                return invalid(
                        "Duplicate load cargo line ID"
                );
            }
        }

        Map<UUID, Set<Long>> placedUnits =
                new HashMap<>();

        for (LoadCargoPlacement placement : placements) {
            if (placement == null) {
                return invalid(
                        "Cargo placement cannot be null"
                );
            }

            LoadCargoLine owner = linesById.get(
                    placement.loadCargoLineId()
            );

            if (owner == null) {
                return invalid(
                        "Placement references an unknown cargo line"
                );
            }

            if (!owner.cargoId().equals(
                    placement.cargoId()
            )) {
                return invalid(
                        "Placement cargo does not match its cargo line"
                );
            }

            long index = placement.unitIndex();

            if (index >= owner.quantity()) {
                return invalid(
                        "Placement unit index exceeds line quantity"
                );
            }

            Set<Long> indices = placedUnits.computeIfAbsent(
                    owner.id(),
                    ignored -> new HashSet<>()
            );

            if (!indices.add(index)) {
                return invalid(
                        "Duplicate unit index within cargo line"
                );
            }
        }

        for (LoadCargoLine line : lines) {
            Set<Long> indices = placedUnits.get(
                    line.id()
            );

            if (indices == null
                    || indices.size() != line.quantity()) {
                return invalid(
                        "Cargo line does not have all required placements"
                );
            }
        }

        return valid(
                "All cargo placements have valid ownership and quantity"
        );
    }

    public static Result validateCollisions(
            List<LoadCargoLine> lines,
            List<LoadCargoPlacement> placements
    ) {
        Result ownership = validateOwnership(
                lines,
                placements
        );

        if (ownership.status() != Status.VALID) {
            return ownership;
        }

        Map<UUID, Dimensions> dimensionsByLine =
                new HashMap<>();

        for (LoadCargoLine line : lines) {
            CargoMeasurements measurements =
                    line.cargoLine()
                            .cargo()
                            .getMeasurements();

            if (measurements == null
                    || !measurements.hasDimensions()) {
                return indeterminate(
                        "Cargo dimensions are unknown for line "
                                + line.id()
                );
            }

            dimensionsByLine.put(
                    line.id(),
                    measurements.dimensions()
            );
        }

        for (int i = 0; i < placements.size(); i++) {
            LoadCargoPlacement first =
                    placements.get(i);

            for (int j = i + 1; j < placements.size(); j++) {
                LoadCargoPlacement second =
                        placements.get(j);

                CargoPlacement a =
                        first.placement();

                CargoPlacement b =
                        second.placement();

                // Different cargo spaces have independent
                // coordinate systems and cannot collide.
                if (!a.cargoSpaceId().equals(
                        b.cargoSpaceId()
                )) {
                    continue;
                }

                Dimensions firstDimensions =
                        dimensionsByLine.get(
                                first.loadCargoLineId()
                        );

                Dimensions secondDimensions =
                        dimensionsByLine.get(
                                second.loadCargoLineId()
                        );

                long aLength = placedLength(
                        a,
                        firstDimensions
                );

                long aWidth = placedWidth(
                        a,
                        firstDimensions
                );

                long bLength = placedLength(
                        b,
                        secondDimensions
                );

                long bWidth = placedWidth(
                        b,
                        secondDimensions
                );

                boolean overlapX = intervalsOverlap(
                        a.xMm(),
                        aLength,
                        b.xMm(),
                        bLength
                );

                boolean overlapY = intervalsOverlap(
                        a.yMm(),
                        aWidth,
                        b.yMm(),
                        bWidth
                );

                boolean overlapZ = intervalsOverlap(
                        a.zMm(),
                        firstDimensions.heightMm(),
                        b.zMm(),
                        secondDimensions.heightMm()
                );

                if (overlapX && overlapY && overlapZ) {
                    return invalid(
                            "Cargo placements overlap between load lines"
                    );
                }
            }
        }

        return valid(
                "No cargo placement collisions detected"
        );
    }

    public static Result validateBoundaries(
            List<LoadCargoLine> lines,
            List<LoadCargoPlacement> placements,
            Map<UUID, CargoSpaceSpecifications> cargoSpaces
    ) {
        Objects.requireNonNull(
                cargoSpaces,
                "Cargo spaces are required"
        );

        Result ownership = validateOwnership(
                lines,
                placements
        );

        if (ownership.status() != Status.VALID) {
            return ownership;
        }

        Map<UUID, LoadCargoLine> linesById =
                new HashMap<>();

        for (LoadCargoLine line : lines) {
            linesById.put(
                    line.id(),
                    line
            );
        }

        for (LoadCargoPlacement loadPlacement : placements) {
            CargoPlacement placement =
                    loadPlacement.placement();

            CargoSpaceSpecifications space =
                    cargoSpaces.get(
                            placement.cargoSpaceId()
                    );

            if (space == null) {
                return invalid(
                        "Placement references an unknown cargo space"
                );
            }

            LoadCargoLine line =
                    linesById.get(
                            loadPlacement.loadCargoLineId()
                    );

            CargoMeasurements measurements =
                    line.cargoLine()
                            .cargo()
                            .getMeasurements();

            if (measurements == null
                    || !measurements.hasDimensions()) {
                return indeterminate(
                        "Cargo dimensions are unknown for line "
                                + line.id()
                );
            }

            Dimensions dimensions =
                    measurements.dimensions();

            long length = placedLength(
                    placement,
                    dimensions
            );

            long width = placedWidth(
                    placement,
                    dimensions
            );

            // Subtraction avoids coordinate + dimension overflow.
            if (length > space.usableLengthMm()
                    || placement.xMm()
                    > space.usableLengthMm() - length) {
                return invalid(
                        "Cargo placement exceeds length boundary"
                );
            }

            if (width > space.usableWidthMm()
                    || placement.yMm()
                    > space.usableWidthMm() - width) {
                return invalid(
                        "Cargo placement exceeds width boundary"
                );
            }

            if (space.hasDefinedHeight()
                    && (dimensions.heightMm()
                    > space.usableHeightMm()
                    || placement.zMm()
                    > space.usableHeightMm()
                    - dimensions.heightMm())) {
                return invalid(
                        "Cargo placement exceeds height boundary"
                );
            }
        }

        return valid(
                "All cargo placements remain within their assigned spaces"
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

    private static boolean intervalsOverlap(
            long firstStart,
            long firstSize,
            long secondStart,
            long secondSize
    ) {
        if (firstStart <= secondStart) {
            return secondStart - firstStart < firstSize;
        }

        return firstStart - secondStart < secondSize;
    }

    private static Result valid(String reason) {
        return new Result(
                Status.VALID,
                reason
        );
    }

    private static Result invalid(String reason) {
        return new Result(
                Status.INVALID,
                reason
        );
    }

    private static Result indeterminate(String reason) {
        return new Result(
                Status.INDETERMINATE,
                reason
        );
    }
}
