package com.charron.api.movement.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ManifestLifecycle {

    private final UUID manifestId;
    private final ManifestStatus status;
    private final Instant generatedAt;
    private final List<ManifestLifecycleEvent> events;

    private ManifestLifecycle(
            UUID manifestId,
            ManifestStatus status,
            Instant generatedAt,
            List<ManifestLifecycleEvent> events
    ) {
        this.manifestId = manifestId;
        this.status = status;
        this.generatedAt = generatedAt;
        this.events = List.copyOf(events);
    }

    public static ManifestLifecycle reconstruct(
            MovementManifest manifest,
            List<ManifestLifecycleEvent> events
    ) {

        Objects.requireNonNull(
                manifest,
                "Manifest is required"
        );

        Objects.requireNonNull(
                events,
                "Lifecycle events are required"
        );

        Set<UUID> eventIds = new HashSet<>();

        for (ManifestLifecycleEvent event : events) {

            Objects.requireNonNull(
                    event,
                    "Lifecycle event cannot be null"
            );

            if (!manifest.id().equals(event.manifestId())) {

                throw new IllegalArgumentException(
                        "Event belongs to another manifest"
                );
            }

            if (!eventIds.add(event.id())) {

                throw new IllegalArgumentException(
                        "Duplicate lifecycle event"
                );
            }

            if (event.recordedAt().isBefore(
                    manifest.generatedAt()
            )) {

                throw new IllegalArgumentException(
                        "Lifecycle event cannot precede manifest generation"
                );
            }
        }

        List<ManifestLifecycleEvent> orderedEvents =
                events.stream()
                        .sorted(
                                Comparator.comparing(
                                        ManifestLifecycleEvent::recordedAt
                                ).thenComparingInt(
                                        event -> event.previousStatus()
                                                .ordinal()
                                )
                        )
                        .toList();

        ManifestStatus currentStatus =
                ManifestStatus.DRAFT;

        for (ManifestLifecycleEvent event : orderedEvents) {

            if (event.previousStatus() != currentStatus) {

                throw new IllegalArgumentException(
                        "Inconsistent manifest lifecycle history: "
                                + "expected " + currentStatus
                                + " but found "
                                + event.previousStatus()
                );
            }

            currentStatus = event.newStatus();
        }

        return new ManifestLifecycle(
                manifest.id(),
                currentStatus,
                manifest.generatedAt(),
                orderedEvents
        );
    }

    public UUID manifestId() {
        return manifestId;
    }

    public ManifestStatus status() {
        return status;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<ManifestLifecycleEvent> events() {
        return events;
    }

    public Instant lastTransitionAt() {

        if (events.isEmpty()) {
            return generatedAt;
        }

        return events.getLast().recordedAt();
    }

    public boolean canTransitionTo(ManifestStatus target) {

        return status.canTransitionTo(target);
    }

    public boolean isIssued() {
        return status.isIssued();
    }

    public boolean isTerminal() {
        return status.isTerminal();
    }
}