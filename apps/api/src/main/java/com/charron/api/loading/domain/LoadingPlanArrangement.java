
package com.charron.api.loading.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public record LoadingPlanArrangement(
        UUID loadingPlanId,
        int formatVersion,
        String requestSnapshot,
        String previewSnapshot,
        String status,
        Instant generatedAt,
        Instant savedAt,
        Instant updatedAt
) {
    public LoadingPlanArrangement {
        Objects.requireNonNull(
                loadingPlanId,
                "Loading plan ID is required"
        );
        Objects.requireNonNull(
                requestSnapshot,
                "Request snapshot is required"
        );
        Objects.requireNonNull(
                previewSnapshot,
                "Preview snapshot is required"
        );
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(
                generatedAt,
                "Generation timestamp is required"
        );
        Objects.requireNonNull(
                savedAt,
                "Save timestamp is required"
        );
        Objects.requireNonNull(
                updatedAt,
                "Update timestamp is required"
        );

        if (formatVersion <= 0) {
            throw new IllegalArgumentException(
                    "Format version must be positive"
            );
        }

        if (!status.equals("VALID")
                && !status.equals("INVALID")
                && !status.equals("INDETERMINATE")
                && !status.equals("NOT_PLACED")) {
            throw new IllegalArgumentException(
                    "Unsupported loading status: " + status
            );
        }

        if (requestSnapshot.isBlank()
                || previewSnapshot.isBlank()) {
            throw new IllegalArgumentException(
                    "Arrangement snapshots cannot be blank"
            );
        }

        generatedAt = generatedAt.truncatedTo(ChronoUnit.MICROS);
        savedAt = savedAt.truncatedTo(ChronoUnit.MICROS);
        updatedAt = updatedAt.truncatedTo(ChronoUnit.MICROS);

        if (updatedAt.isBefore(savedAt)) {
            throw new IllegalArgumentException(
                    "Update timestamp cannot precede save timestamp"
            );
        }
    }

    public static LoadingPlanArrangement create(
            UUID loadingPlanId,
            String requestSnapshot,
            String previewSnapshot,
            String status
    ) {
        Instant now = Instant.now()
                .truncatedTo(ChronoUnit.MICROS);

        return new LoadingPlanArrangement(
                loadingPlanId,
                1,
                requestSnapshot,
                previewSnapshot,
                status,
                now,
                now,
                now
        );
    }
}
