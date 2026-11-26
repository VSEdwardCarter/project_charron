package com.charron.api.movement.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MovementQualityVerification(
        UUID id,
        UUID manifestId,
        UUID reviewerId,
        boolean equipmentAssignmentVerified,
        boolean equipmentInspectionCompleted,
        boolean cargoReconciliationVerified,
        boolean loadingArrangementVerified,
        boolean cargoSecurementVerified,
        boolean documentationVerified,
        Instant verifiedAt,
        String remarks
) {

    public MovementQualityVerification {

        Objects.requireNonNull(id, "Verification ID is required");
        Objects.requireNonNull(manifestId, "Manifest ID is required");
        Objects.requireNonNull(reviewerId, "Reviewer ID is required");
        Objects.requireNonNull(verifiedAt, "Verification timestamp is required");

        if (!equipmentAssignmentVerified
                || !equipmentInspectionCompleted
                || !cargoReconciliationVerified
                || !loadingArrangementVerified
                || !cargoSecurementVerified
                || !documentationVerified) {

            throw new IllegalArgumentException(
                    "All required quality checks must be completed"
            );
        }

        if (remarks != null) {
            remarks = remarks.strip();

            if (remarks.isEmpty()) {
                remarks = null;
            }
        }
    }
}