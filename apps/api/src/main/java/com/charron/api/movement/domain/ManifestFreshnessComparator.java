package com.charron.api.movement.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ManifestFreshnessComparator {

    private ManifestFreshnessComparator() {
    }

    public static ManifestFreshnessAssessment compare(
            MovementManifest persisted,
            MovementManifest current
    ) {

        Objects.requireNonNull(
                persisted,
                "Persisted manifest is required"
        );

        Objects.requireNonNull(
                current,
                "Current manifest snapshot is required"
        );

        if (!persisted.movementId().equals(
                current.movementId()
        )) {

            throw new IllegalArgumentException(
                    "Cannot compare manifests belonging "
                            + "to different movements"
            );
        }

        EnumSet<ManifestChangeType> changes =
                EnumSet.noneOf(
                        ManifestChangeType.class
                );

        if (!persisted.movementReference().equals(
                current.movementReference()
        )) {

            changes.add(
                    ManifestChangeType.MOVEMENT_REFERENCE
            );
        }

        if (!persisted.origin().equals(
                current.origin()
        )) {

            changes.add(
                    ManifestChangeType.ORIGIN
            );
        }

        if (!persisted.destination().equals(
                current.destination()
        )) {

            changes.add(
                    ManifestChangeType.DESTINATION
            );
        }

        if (!persisted.plannedDeparture().equals(
                current.plannedDeparture()
        )) {

            changes.add(
                    ManifestChangeType.PLANNED_DEPARTURE
            );
        }

        if (!persisted.plannedArrival().equals(
                current.plannedArrival()
        )) {

            changes.add(
                    ManifestChangeType.PLANNED_ARRIVAL
            );
        }

        if (!sameContents(
                persisted.equipment(),
                current.equipment()
        )) {

            changes.add(
                    ManifestChangeType.EQUIPMENT
            );
        }

        if (!sameContents(
                persisted.cargo(),
                current.cargo()
        )) {

            changes.add(
                    ManifestChangeType.CARGO
            );
        }

        if (!sameContents(
                persisted.loadingPlans(),
                current.loadingPlans()
        )) {

            changes.add(
                    ManifestChangeType.LOADING_PLANS
            );
        }

        ManifestFreshnessStatus status =
                changes.isEmpty()
                        ? ManifestFreshnessStatus.CURRENT
                        : ManifestFreshnessStatus.STALE;

        return new ManifestFreshnessAssessment(
                persisted.id(),
                status,
                changes,
                Instant.now().truncatedTo(
                        ChronoUnit.MICROS
                ),
                null
        );
    }

    private static <T> boolean sameContents(
            List<T> first,
            List<T> second
    ) {

        if (first.size() != second.size()) {
            return false;
        }

        return frequencies(first).equals(
                frequencies(second)
        );
    }

    private static <T> Map<T, Integer> frequencies(
            List<T> entries
    ) {

        Map<T, Integer> counts = new HashMap<>();

        for (T entry : entries) {

            counts.merge(
                    entry,
                    1,
                    Integer::sum
            );
        }

        return counts;
    }
}