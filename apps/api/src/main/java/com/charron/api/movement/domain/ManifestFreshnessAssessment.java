package com.charron.api.movement.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ManifestFreshnessAssessment(
        UUID manifestId,
        ManifestFreshnessStatus status,
        Set<ManifestChangeType> changes,
        Instant assessedAt,
        String reason
) {

    public ManifestFreshnessAssessment {

        Objects.requireNonNull(manifestId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(changes);
        Objects.requireNonNull(assessedAt);

        changes = Set.copyOf(changes);

        if (reason != null) {

            reason = reason.strip();

            if (reason.isEmpty()) {
                reason = null;
            }
        }

        switch (status) {

            case CURRENT -> {

                if (!changes.isEmpty() || reason != null) {

                    throw new IllegalArgumentException(
                            "Current manifest cannot contain "
                                    + "changes or failure reasons"
                    );
                }
            }

            case STALE -> {

                if (changes.isEmpty() || reason != null) {

                    throw new IllegalArgumentException(
                            "Stale manifest requires detected changes "
                                    + "and cannot contain a failure reason"
                    );
                }
            }

            case UNVERIFIABLE -> {

                if (!changes.isEmpty() || reason == null) {

                    throw new IllegalArgumentException(
                            "Unverifiable manifest requires a reason "
                                    + "and cannot contain confirmed changes"
                    );
                }
            }
        }
    }

    public boolean isCurrent() {

        return status == ManifestFreshnessStatus.CURRENT;
    }

    public boolean isStale() {

        return status == ManifestFreshnessStatus.STALE;
    }

    public boolean isVerifiable() {

        return status != ManifestFreshnessStatus.UNVERIFIABLE;
    }
}