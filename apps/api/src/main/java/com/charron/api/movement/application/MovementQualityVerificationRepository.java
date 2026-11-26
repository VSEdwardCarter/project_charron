package com.charron.api.movement.application;

import com.charron.api.movement.domain.MovementQualityVerification;

import java.util.List;
import java.util.UUID;

public interface MovementQualityVerificationRepository {

    void save(MovementQualityVerification verification);

    List<MovementQualityVerification> findByManifestId(
            UUID manifestId
    );

    List<MovementQualityVerification> findByReviewerId(
            UUID reviewerId
    );

    boolean existsByManifestId(UUID manifestId);

    boolean existsForCurrentOperationalRevision(
            UUID manifestId
    );
}