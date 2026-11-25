package com.charron.api.movement.application;

import java.util.UUID;

public class ManifestLifecycleConflictException
        extends RuntimeException {

    private final UUID manifestId;

    public ManifestLifecycleConflictException(
            UUID manifestId,
            String message
    ) {

        super(message);

        this.manifestId = manifestId;
    }

    public UUID getManifestId() {
        return manifestId;
    }
}