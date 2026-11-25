package com.charron.api.movement.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ManifestLifecycleEvent(
        UUID id,
        UUID manifestId,
        ManifestStatus previousStatus,
        ManifestStatus newStatus,
        UUID actorId,
        Instant recordedAt,
        String remarks
) {

    public ManifestLifecycleEvent {

        Objects.requireNonNull(
                id,
                "Event ID is required"
        );

        Objects.requireNonNull(
                manifestId,
                "Manifest ID is required"
        );

        Objects.requireNonNull(
                previousStatus,
                "Previous status is required"
        );

        Objects.requireNonNull(
                newStatus,
                "New status is required"
        );

        Objects.requireNonNull(
                actorId,
                "Actor ID is required"
        );

        Objects.requireNonNull(
                recordedAt,
                "Recorded timestamp is required"
        );

        if (!previousStatus.canTransitionTo(newStatus)) {

            throw new IllegalArgumentException(
                    "Invalid manifest lifecycle transition: "
                            + previousStatus
                            + " -> "
                            + newStatus
            );
        }

        if (remarks != null) {

            remarks = remarks.strip();

            if (remarks.isEmpty()) {
                remarks = null;
            }
        }
    }
}