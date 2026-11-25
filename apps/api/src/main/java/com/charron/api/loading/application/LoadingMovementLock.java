package com.charron.api.loading.application;

import java.util.UUID;

public interface LoadingMovementLock {

    /**
     * Acquires an exclusive lock on the parent movement
     * within the current transaction.
     *
     * Returns false if the movement does not exist.
     */
    boolean acquire(UUID movementId);
}