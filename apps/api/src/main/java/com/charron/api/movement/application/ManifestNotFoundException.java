package com.charron.api.movement.application;

import java.util.UUID;

public class ManifestNotFoundException
        extends IllegalArgumentException {

    private final UUID manifestId;

    public ManifestNotFoundException(UUID manifestId) {

        super("Manifest not found: " + manifestId);

        this.manifestId = manifestId;
    }

    public UUID getManifestId() {
        return manifestId;
    }
}