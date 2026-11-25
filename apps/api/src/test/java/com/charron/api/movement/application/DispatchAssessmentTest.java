
package com.charron.api.movement.application;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DispatchAssessmentTest {

    private final UUID movementId = UUID.randomUUID();

    private DispatchAssessment.Finding finding(
            String code,
            DispatchAssessment.Severity severity
    ) {
        return new DispatchAssessment.Finding(
                code,
                severity,
                "Assessment finding: " + code
        );
    }

    @Test
    void emptyAssessmentHasNoBlockingFindings() {
        DispatchAssessment assessment = new DispatchAssessment(
                movementId,
                List.of()
        );

        assertFalse(assessment.hasBlockingFindings());
        assertFalse(assessment.requiresAttention());
    }

    @Test
    void informationalFindingDoesNotBlock() {
        DispatchAssessment assessment = new DispatchAssessment(
                movementId,
                List.of(finding(
                        "DOCUMENTS_GENERATED",
                        DispatchAssessment.Severity.INFO
                ))
        );

        assertFalse(assessment.hasBlockingFindings());
        assertTrue(assessment.requiresAttention());
    }

    @Test
    void warningRequiresAttentionButDoesNotBlock() {
        DispatchAssessment assessment = new DispatchAssessment(
                movementId,
                List.of(finding(
                        "REVIEW_REQUIRED",
                        DispatchAssessment.Severity.WARNING
                ))
        );

        assertFalse(assessment.hasBlockingFindings());
        assertTrue(assessment.requiresAttention());
    }

    @Test
    void blockerPreventsReadiness() {
        DispatchAssessment assessment = new DispatchAssessment(
                movementId,
                List.of(finding(
                        "LOADING_ARRANGEMENT_STALE",
                        DispatchAssessment.Severity.BLOCKER
                ))
        );

        assertTrue(assessment.hasBlockingFindings());
        assertTrue(assessment.requiresAttention());
    }

    @Test
    void detectsBlockerAmongMultipleFindings() {
        DispatchAssessment assessment = new DispatchAssessment(
                movementId,
                List.of(
                        finding(
                                "EQUIPMENT_ASSIGNED",
                                DispatchAssessment.Severity.INFO
                        ),
                        finding(
                                "DOCUMENT_REVIEW",
                                DispatchAssessment.Severity.WARNING
                        ),
                        finding(
                                "CARGO_MISSING",
                                DispatchAssessment.Severity.BLOCKER
                        )
                )
        );

        assertTrue(assessment.hasBlockingFindings());
        assertEquals(3, assessment.findings().size());
    }

    @Test
    void findingsAreImmutable() {
        List<DispatchAssessment.Finding> source =
                new ArrayList<>();

        source.add(finding(
                "EQUIPMENT_CHECK",
                DispatchAssessment.Severity.INFO
        ));

        DispatchAssessment assessment = new DispatchAssessment(
                movementId,
                source
        );

        source.clear();

        assertEquals(1, assessment.findings().size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> assessment.findings().clear()
        );
    }

    @Test
    void rejectsNullMovementId() {
        assertThrows(
                NullPointerException.class,
                () -> new DispatchAssessment(
                        null,
                        List.of()
                )
        );
    }

    @Test
    void rejectsNullFindings() {
        assertThrows(
                NullPointerException.class,
                () -> new DispatchAssessment(
                        movementId,
                        null
                )
        );
    }

    @Test
    void rejectsInvalidFinding() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DispatchAssessment.Finding(
                        " ",
                        DispatchAssessment.Severity.BLOCKER,
                        "Invalid code"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new DispatchAssessment.Finding(
                        "INVALID",
                        DispatchAssessment.Severity.WARNING,
                        " "
                )
        );
    }

    @Test
    void rejectsNullFindingSeverity() {
        assertThrows(
                NullPointerException.class,
                () -> new DispatchAssessment.Finding(
                        "INVALID",
                        null,
                        "Missing severity"
                )
        );
    }
}
