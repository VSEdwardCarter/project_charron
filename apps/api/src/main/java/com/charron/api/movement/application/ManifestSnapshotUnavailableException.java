package com.charron.api.movement.application;

public class ManifestSnapshotUnavailableException
        extends IllegalStateException {

    public ManifestSnapshotUnavailableException(
            String message
    ) {

        super(message);
    }
}