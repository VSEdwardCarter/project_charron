
package com.charron.api.movement.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record MovementManifest(
        UUID id,
        UUID movementId,
        int version,
        String movementReference,
        String origin,
        String destination,
        Instant plannedDeparture,
        Instant plannedArrival,
        List<EquipmentEntry> equipment,
        List<CargoEntry> cargo,
        List<LoadingEntry> loadingPlans,
        Instant generatedAt
) {

    public MovementManifest {
        Objects.requireNonNull(id, "Manifest ID is required");
        Objects.requireNonNull(movementId, "Movement ID is required");
        Objects.requireNonNull(
                movementReference,
                "Movement reference is required"
        );
        Objects.requireNonNull(origin, "Origin is required");
        Objects.requireNonNull(destination, "Destination is required");
        Objects.requireNonNull(plannedDeparture);
        Objects.requireNonNull(plannedArrival);
        Objects.requireNonNull(generatedAt);

        if (version < 1) {
            throw new IllegalArgumentException(
                    "Manifest version must be positive"
            );
        }

        equipment = List.copyOf(
                Objects.requireNonNull(equipment)
        );

        cargo = List.copyOf(
                Objects.requireNonNull(cargo)
        );

        loadingPlans = List.copyOf(
                Objects.requireNonNull(loadingPlans)
        );
    }

    public record EquipmentEntry(
            UUID equipmentId,
            String name,
            MovementEquipmentRole role,
            String vin
    ) {
        public EquipmentEntry {
            Objects.requireNonNull(equipmentId);
            Objects.requireNonNull(name);
            Objects.requireNonNull(role);
        }
    }

    public record CargoEntry(
            UUID loadingPlanId,
            UUID cargoId,
            String name,
            int quantity,
            Long lengthMm,
            Long widthMm,
            Long heightMm,
            Long unitWeightGrams
    ) {
        public CargoEntry {
            Objects.requireNonNull(loadingPlanId);
            Objects.requireNonNull(cargoId);
            Objects.requireNonNull(name);

            if (quantity <= 0) {
                throw new IllegalArgumentException(
                        "Manifest cargo quantity must be positive"
                );
            }
        }

        public Long totalWeightGrams() {
            if (unitWeightGrams == null) {
                return null;
            }

            return Math.multiplyExact(
                    unitWeightGrams,
                    (long) quantity
            );
        }
    }

    public record LoadingEntry(
            UUID loadingPlanId,
            UUID equipmentId,
            UUID cargoSpaceId,
            String arrangementStatus,
            String arrangementSnapshot
    ) {
        public LoadingEntry {
            Objects.requireNonNull(loadingPlanId);
            Objects.requireNonNull(equipmentId);
            Objects.requireNonNull(cargoSpaceId);
            Objects.requireNonNull(arrangementStatus);
            Objects.requireNonNull(arrangementSnapshot);
        }
    }
}
