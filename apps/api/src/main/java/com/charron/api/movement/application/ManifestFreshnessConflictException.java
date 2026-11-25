package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestFreshnessAssessment;

import java.util.Objects;
import java.util.UUID;

public class ManifestFreshnessConflictException
        extends ManifestLifecycleConflictException {

    private final ManifestFreshnessAssessment assessment;

    public ManifestFreshnessConflictException(
            ManifestFreshnessAssessment assessment
    ) {

        super(
                Objects.requireNonNull(assessment).manifestId(),
                buildMessage(assessment)
        );

        if (assessment.isCurrent()) {

            throw new IllegalArgumentException(
                    "A current manifest cannot produce "
                            + "a freshness conflict"
            );
        }

        this.assessment = assessment;
    }

    public ManifestFreshnessAssessment getAssessment() {

        return assessment;
    }

    private static String buildMessage(
            ManifestFreshnessAssessment assessment
    ) {

        return switch (assessment.status()) {

            case STALE ->
                    "Manifest is stale. Detected changes: "
                            + assessment.changes();

            case UNVERIFIABLE ->
                    "Manifest freshness cannot be verified: "
                            + assessment.reason();

            case CURRENT ->
                    "Manifest is current";
        };
    }
}