
package com.charron.api.fleet.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EquipmentModificationEvent(
        UUID id,
        UUID modificationId,
        EquipmentModificationEventType type,
        Instant effectiveAt,
        Instant recordedAt
) {

    public EquipmentModificationEvent {
        Objects.requireNonNull(
                id, "Event ID is required"
        );

        Objects.requireNonNull(
                modificationId, "Modification ID is required"
        );

        Objects.requireNonNull(
                type, "Event type is required"
        );

        Objects.requireNonNull(
                effectiveAt, "Effective date is required"
        );

        Objects.requireNonNull(
                recordedAt, "Recorded date is required"
        );
    }
}
