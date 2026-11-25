package com.charron.api.movement.application;

import com.charron.api.movement.domain.MovementManifest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovementManifestRepository {

    void save(MovementManifest manifest);

    Optional<MovementManifest> findById(UUID manifestId);

    Optional<MovementManifest> findByMovementIdAndVersion(
            UUID movementId,
            int version
    );

    Optional<MovementManifest> findLatestByMovementId(
            UUID movementId
    );

    List<MovementManifest> findByMovementId(
            UUID movementId
    );

    int findLatestVersion(UUID movementId);
}
