
package com.charron.api.fleet.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;


public record EquipmentModification(
        UUID id,
        UUID equipmentId,
        UUID configurationId,
        EquipmentModificationType type,
        String description,
        Instant effectiveAt,
        Instant recordedAt,
        Instant removedAt
) {

    public EquipmentModification {
        Objects.requireNonNull(id, "Modification ID is required");
        Objects.requireNonNull(equipmentId, "Equipment ID is required");
        Objects.requireNonNull(type, "Modification type is required");
        Objects.requireNonNull(effectiveAt, "Effective date is required");
        Objects.requireNonNull(recordedAt, "Recorded date is required");

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Modification description is required"
            );
        }

        description = description.trim();

        if (removedAt != null && removedAt.isBefore(effectiveAt)) {
            throw new IllegalArgumentException(
                    "Removal date cannot precede effective date"
            );
        }
    }

    // Preserve existing constructor compatibility.
    public EquipmentModification(
            UUID id,
            UUID equipmentId,
            UUID configurationId,
            EquipmentModificationType type,
            String description,
            Instant effectiveAt,
            Instant recordedAt
    ) {
        this(
                id,
                equipmentId,
                configurationId,
                type,
                description,
                effectiveAt,
                recordedAt,
                null
        );
    }

    public boolean isEffectiveAt(Instant timestamp) {
        Objects.requireNonNull(timestamp, "Timestamp is required");

        return !timestamp.isBefore(effectiveAt)
                && (removedAt == null || timestamp.isBefore(removedAt));
    }
}

