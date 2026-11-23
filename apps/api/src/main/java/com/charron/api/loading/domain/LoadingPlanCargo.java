
package com.charron.api.loading.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public record LoadingPlanCargo(
        UUID id,
        UUID loadingPlanId,
        UUID cargoId,
        int quantity,
        Instant createdAt,
        Instant updatedAt
) {
    public LoadingPlanCargo {
        Objects.requireNonNull(id, "Cargo line ID is required");
        Objects.requireNonNull(loadingPlanId, "Loading plan ID is required");
        Objects.requireNonNull(cargoId, "Cargo ID is required");
        Objects.requireNonNull(createdAt, "Creation timestamp is required");
        Objects.requireNonNull(updatedAt, "Update timestamp is required");

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Cargo quantity must be greater than zero"
            );
        }

        createdAt = createdAt.truncatedTo(ChronoUnit.MICROS);
        updatedAt = updatedAt.truncatedTo(ChronoUnit.MICROS);

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "Update timestamp cannot precede creation"
            );
        }
    }

    public static LoadingPlanCargo create(
            UUID loadingPlanId,
            UUID cargoId,
            int quantity
    ) {
        Instant now = Instant.now()
                .truncatedTo(ChronoUnit.MICROS);

        return new LoadingPlanCargo(
                UUID.randomUUID(),
                loadingPlanId,
                cargoId,
                quantity,
                now,
                now
        );
    }

    public LoadingPlanCargo withQuantity(int newQuantity) {
        return new LoadingPlanCargo(
                id,
                loadingPlanId,
                cargoId,
                newQuantity,
                createdAt,
                Instant.now().truncatedTo(ChronoUnit.MICROS)
        );
    }
}
