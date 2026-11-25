
package com.charron.api.movement.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public record Movement(
        UUID id,
        String reference,
        String origin,
        String destination,
        Instant plannedDeparture,
        Instant plannedArrival,
        MovementStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public Movement {
        Objects.requireNonNull(id, "Movement ID is required");
        Objects.requireNonNull(reference, "Movement reference is required");
        Objects.requireNonNull(origin, "Origin is required");
        Objects.requireNonNull(destination, "Destination is required");
        Objects.requireNonNull(plannedDeparture, "Planned departure is required");
        Objects.requireNonNull(plannedArrival, "Planned arrival is required");
        Objects.requireNonNull(status, "Movement status is required");
        Objects.requireNonNull(createdAt, "Creation timestamp is required");
        Objects.requireNonNull(updatedAt, "Update timestamp is required");

        plannedDeparture = plannedDeparture.truncatedTo(ChronoUnit.MICROS);
        plannedArrival = plannedArrival.truncatedTo(ChronoUnit.MICROS);
        createdAt = createdAt.truncatedTo(ChronoUnit.MICROS);
        updatedAt = updatedAt.truncatedTo(ChronoUnit.MICROS);

        reference = reference.trim();
        origin = origin.trim();
        destination = destination.trim();

        if (reference.isBlank()) {
            throw new IllegalArgumentException(
                    "Movement reference cannot be blank"
            );
        }

        if (origin.isBlank() || destination.isBlank()) {
            throw new IllegalArgumentException(
                    "Movement origin and destination cannot be blank"
            );
        }

        if (!plannedArrival.isAfter(plannedDeparture)) {
            throw new IllegalArgumentException(
                    "Planned arrival must be after planned departure"
            );
        }

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "Updated timestamp cannot precede creation"
            );
        }
    }

    public static Movement plan(
            String reference,
            String origin,
            String destination,
            Instant departure,
            Instant arrival
    ) {
        Instant now = Instant.now();

        return new Movement(
                UUID.randomUUID(),
                reference,
                origin,
                destination,
                departure,
                arrival,
                MovementStatus.PLANNED,
                now,
                now
        );
    }

    public Movement transitionTo(MovementStatus targetStatus) {
        Objects.requireNonNull(
                targetStatus,
                "Target movement status is required"
        );

        if (!status.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                    "Invalid movement transition: "
                            + status
                            + " -> "
                            + targetStatus
            );
        }

        Instant now = Instant.now()
                .truncatedTo(ChronoUnit.MICROS);

        // Keep timestamps valid if the system clock moves backward.
        if (now.isBefore(updatedAt)) {
            now = updatedAt;
        }

        return new Movement(
                id,
                reference,
                origin,
                destination,
                plannedDeparture,
                plannedArrival,
                targetStatus,
                createdAt,
                now
        );
    }
}
