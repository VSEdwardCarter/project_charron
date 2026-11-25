package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestLifecycleEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManifestLifecycleEventRepository {

    /**
     * Persists a new immutable lifecycle event.
     * Existing events must never be overwritten.
     */
    void save(ManifestLifecycleEvent event);

    /**
     * Retrieves an individual lifecycle event.
     */
    Optional<ManifestLifecycleEvent> findById(UUID eventId);

    /**
     * Retrieves the complete lifecycle history
     * for a manifest in chronological order.
     *
     * Returns an empty list when no events exist.
     */
    List<ManifestLifecycleEvent> findByManifestId(
            UUID manifestId
    );
}