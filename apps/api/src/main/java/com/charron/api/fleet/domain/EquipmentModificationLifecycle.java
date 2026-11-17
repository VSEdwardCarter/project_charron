
package com.charron.api.fleet.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class EquipmentModificationLifecycle {

    private final UUID modificationId;
    private final Instant effectiveAt;
    private final Instant removedAt;

    private EquipmentModificationLifecycle(
            UUID modificationId,
            Instant effectiveAt,
            Instant removedAt
    ) {
        this.modificationId = modificationId;
        this.effectiveAt = effectiveAt;
        this.removedAt = removedAt;
    }

    public static EquipmentModificationLifecycle reconstruct(
            UUID modificationId,
            List<EquipmentModificationEvent> events
    ) {
        Objects.requireNonNull(
                modificationId, "Modification ID is required"
        );
        Objects.requireNonNull(events, "Events are required");

        Instant installation = null;
        Instant removal = null;

        for (EquipmentModificationEvent event : events) {
            Objects.requireNonNull(event, "Event cannot be null");

            if (!modificationId.equals(event.modificationId())) {
                throw new IllegalArgumentException(
                        "Event belongs to another modification"
                );
            }

            switch (event.type()) {
                case RECORDED -> {
                    if (installation != null) {
                        throw new IllegalArgumentException(
                                "Duplicate initial recording"
                        );
                    }
                    installation = event.effectiveAt();
                }
                case REMOVED -> {
                    if (removal != null) {
                        throw new IllegalArgumentException(
                                "Duplicate removal"
                        );
                    }
                    removal = event.effectiveAt();
                }
            }
        }

        if (installation == null) {
            throw new IllegalArgumentException(
                    "Initial recording event is required"
            );
        }

        if (removal != null && removal.isBefore(installation)) {
            throw new IllegalArgumentException(
                    "Removal cannot precede installation"
            );
        }

        return new EquipmentModificationLifecycle(
                modificationId,
                installation,
                removal
        );
    }

    public UUID modificationId() {
        return modificationId;
    }

    public Instant effectiveAt() {
        return effectiveAt;
    }

    public Instant removedAt() {
        return removedAt;
    }

    public boolean isEffectiveAt(Instant timestamp) {
        Objects.requireNonNull(timestamp, "Timestamp is required");

        return !timestamp.isBefore(effectiveAt)
                && (removedAt == null || timestamp.isBefore(removedAt));
    }


    public static EquipmentModificationLifecycle reconstructAsKnownAt(
            UUID modificationId,
            List<EquipmentModificationEvent> events,
            Instant knownAt
    ) {
        Objects.requireNonNull(
                knownAt,
                "Known-at timestamp is required"
        );

        Objects.requireNonNull(
                events,
                "Events are required"
        );

        List<EquipmentModificationEvent> knownEvents =
                events.stream()
                        .filter(Objects::nonNull)
                        .filter(event ->
                                !event.recordedAt().isAfter(knownAt))
                        .toList();

        return reconstruct(modificationId, knownEvents);
    }

}
