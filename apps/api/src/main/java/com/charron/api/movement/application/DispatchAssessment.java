
package com.charron.api.movement.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record DispatchAssessment(
        UUID movementId,
        List<Finding> findings
) {

    public DispatchAssessment {
        Objects.requireNonNull(
                movementId,
                "Movement ID is required"
        );

        Objects.requireNonNull(
                findings,
                "Assessment findings are required"
        );

        findings = List.copyOf(findings);
    }

    public boolean hasBlockingFindings() {
        return findings.stream()
                .anyMatch(finding ->
                        finding.severity() == Severity.BLOCKER
                );
    }

    public boolean requiresAttention() {
        return !findings.isEmpty();
    }

    public enum Severity {
        INFO,
        WARNING,
        BLOCKER
    }

    public record Finding(
            String code,
            Severity severity,
            String description
    ) {
        public Finding {
            Objects.requireNonNull(code, "Finding code is required");
            Objects.requireNonNull(severity, "Severity is required");
            Objects.requireNonNull(description, "Description is required");

            code = code.trim();
            description = description.trim();

            if (code.isBlank() || description.isBlank()) {
                throw new IllegalArgumentException(
                        "Finding code and description cannot be blank"
                );
            }
        }
    }
}
