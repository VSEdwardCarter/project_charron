package com.charron.api.loading.infrastructure;

import com.charron.api.loading.application.LoadingMovementLock;
import com.charron.api.movement.application.MovementRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Component
public class LoadingMovementLockAdapter
        implements LoadingMovementLock {

    private final MovementRepository movementRepository;

    public LoadingMovementLockAdapter(
            MovementRepository movementRepository
    ) {

        this.movementRepository =
                Objects.requireNonNull(movementRepository);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean acquire(UUID movementId) {

        Objects.requireNonNull(
                movementId,
                "Movement ID is required"
        );

        return movementRepository
                .findByIdForUpdate(movementId)
                .isPresent();
    }
}