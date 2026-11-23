
package com.charron.api.loading.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public record LoadingPlan(
        UUID id,
        UUID movementId,
        UUID equipmentId,
        UUID cargoSpaceId,
        Instant createdAt,
        Instant updatedAt
) {
    public LoadingPlan {
        Objects.requireNonNull(id, "Loading plan ID is required");
        Objects.requireNonNull(movementId, "Movement ID is required");
        Objects.requireNonNull(equipmentId, "Equipment ID is required");
        Objects.requireNonNull(cargoSpaceId, "Cargo space ID is required");
        Objects.requireNonNull(createdAt, "Creation timestamp is required");
        Objects.requireNonNull(updatedAt, "Update timestamp is required");

        createdAt = createdAt.truncatedTo(ChronoUnit.MICROS);
        updatedAt = updatedAt.truncatedTo(ChronoUnit.MICROS);

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "Update timestamp cannot precede creation"
            );
        }
    }

    public static LoadingPlan create(
            UUID movementId,
            UUID equipmentId,
            UUID cargoSpaceId
    ) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        return new LoadingPlan(
                UUID.randomUUID(),
                movementId,
                equipmentId,
                cargoSpaceId,
                now,
                now
        );
    }
}
