package com.charron.api.movement.application;

import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovementRepository {

    void save(Movement movement);

    Optional<Movement> findById(UUID id);

    /**
     * Retrieves a movement and acquires an exclusive
     * database row lock.
     *
     * Must be called within an active write transaction.
     * The lock is retained until the transaction completes.
     */
    Optional<Movement> findByIdForUpdate(UUID id);

    List<Movement> findAll();

    boolean existsByReference(String reference);

    boolean updateStatus(
            UUID movementId,
            MovementStatus expectedStatus,
            MovementStatus targetStatus,
            Instant updatedAt
    );
}