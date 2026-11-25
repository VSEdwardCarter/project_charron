package com.charron.api.movement.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ManifestFreshnessAssessmentTest {

    private final UUID manifestId = UUID.randomUUID();

    private final Instant assessedAt = Instant.now();

    private ManifestFreshnessAssessment createAssessment(
            ManifestFreshnessStatus status,
            Set<ManifestChangeType> changes,
            String reason
    ) {

        return new ManifestFreshnessAssessment(
                manifestId,
                status,
                changes,
                assessedAt,
                reason
        );
    }

    @Test
    void acceptsCurrentAssessment() {

        ManifestFreshnessAssessment assessment =
                createAssessment(
                        ManifestFreshnessStatus.CURRENT,
                        Set.of(),
                        null
                );

        assertEquals(manifestId, assessment.manifestId());
        assertEquals(assessedAt, assessment.assessedAt());

        assertTrue(assessment.isCurrent());
        assertFalse(assessment.isStale());
        assertTrue(assessment.isVerifiable());

        assertTrue(assessment.changes().isEmpty());
        assertNull(assessment.reason());
    }

    @Test
    void acceptsStaleAssessment() {

        ManifestFreshnessAssessment assessment =
                createAssessment(
                        ManifestFreshnessStatus.STALE,
                        Set.of(ManifestChangeType.CARGO),
                        null
                );

        assertFalse(assessment.isCurrent());
        assertTrue(assessment.isStale());
        assertTrue(assessment.isVerifiable());

        assertEquals(
                Set.of(ManifestChangeType.CARGO),
                assessment.changes()
        );

        assertNull(assessment.reason());
    }

    @Test
    void acceptsUnverifiableAssessment() {

        ManifestFreshnessAssessment assessment =
                createAssessment(
                        ManifestFreshnessStatus.UNVERIFIABLE,
                        Set.of(),
                        "Loading arrangement unavailable"
                );

        assertFalse(assessment.isCurrent());
        assertFalse(assessment.isStale());
        assertFalse(assessment.isVerifiable());

        assertTrue(assessment.changes().isEmpty());

        assertEquals(
                "Loading arrangement unavailable",
                assessment.reason()
        );
    }

    @Test
    void acceptsMultipleDetectedChanges() {

        Set<ManifestChangeType> changes = Set.of(
                ManifestChangeType.EQUIPMENT,
                ManifestChangeType.CARGO,
                ManifestChangeType.LOADING_PLANS
        );

        ManifestFreshnessAssessment assessment =
                createAssessment(
                        ManifestFreshnessStatus.STALE,
                        changes,
                        null
                );

        assertEquals(changes, assessment.changes());
        assertEquals(3, assessment.changes().size());
    }

    @Test
    void rejectsCurrentAssessmentWithChanges() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createAssessment(
                        ManifestFreshnessStatus.CURRENT,
                        Set.of(ManifestChangeType.CARGO),
                        null
                )
        );
    }

    @Test
    void rejectsCurrentAssessmentWithFailureReason() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createAssessment(
                        ManifestFreshnessStatus.CURRENT,
                        Set.of(),
                        "Equipment unavailable"
                )
        );
    }

    @Test
    void rejectsStaleAssessmentWithoutChanges() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createAssessment(
                        ManifestFreshnessStatus.STALE,
                        Set.of(),
                        null
                )
        );
    }

    @Test
    void rejectsStaleAssessmentWithFailureReason() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createAssessment(
                        ManifestFreshnessStatus.STALE,
                        Set.of(ManifestChangeType.EQUIPMENT),
                        "Unable to reconstruct movement"
                )
        );
    }

    @Test
    void rejectsUnverifiableAssessmentWithoutReason() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createAssessment(
                        ManifestFreshnessStatus.UNVERIFIABLE,
                        Set.of(),
                        null
                )
        );
    }

    @Test
    void rejectsUnverifiableAssessmentWithBlankReason() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createAssessment(
                        ManifestFreshnessStatus.UNVERIFIABLE,
                        Set.of(),
                        "   "
                )
        );
    }

    @Test
    void rejectsUnverifiableAssessmentWithConfirmedChanges() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createAssessment(
                        ManifestFreshnessStatus.UNVERIFIABLE,
                        Set.of(ManifestChangeType.CARGO),
                        "Unable to reconstruct movement"
                )
        );
    }

    @Test
    void normalizesFailureReasonWhitespace() {

        ManifestFreshnessAssessment assessment =
                createAssessment(
                        ManifestFreshnessStatus.UNVERIFIABLE,
                        Set.of(),
                        "  Loading arrangement unavailable  "
                );

        assertEquals(
                "Loading arrangement unavailable",
                assessment.reason()
        );
    }

    @Test
    void normalizesBlankOptionalReasonToNull() {

        ManifestFreshnessAssessment assessment =
                createAssessment(
                        ManifestFreshnessStatus.CURRENT,
                        Set.of(),
                        "   "
                );

        assertNull(assessment.reason());
    }

    @Test
    void preservesDefensiveCopyOfChanges() {

        Set<ManifestChangeType> changes =
                EnumSet.of(ManifestChangeType.CARGO);

        ManifestFreshnessAssessment assessment =
                createAssessment(
                        ManifestFreshnessStatus.STALE,
                        changes,
                        null
                );

        changes.add(ManifestChangeType.EQUIPMENT);
        changes.clear();

        assertEquals(
                Set.of(ManifestChangeType.CARGO),
                assessment.changes()
        );
    }

    @Test
    void preventsModificationOfAssessmentChanges() {

        ManifestFreshnessAssessment assessment =
                createAssessment(
                        ManifestFreshnessStatus.STALE,
                        Set.of(ManifestChangeType.CARGO),
                        null
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> assessment.changes().add(
                        ManifestChangeType.EQUIPMENT
                )
        );
    }

    @Test
    void rejectsNullManifestIdentity() {

        assertThrows(
                NullPointerException.class,
                () -> new ManifestFreshnessAssessment(
                        null,
                        ManifestFreshnessStatus.CURRENT,
                        Set.of(),
                        assessedAt,
                        null
                )
        );
    }

    @Test
    void rejectsNullFreshnessStatus() {

        assertThrows(
                NullPointerException.class,
                () -> createAssessment(
                        null,
                        Set.of(),
                        null
                )
        );
    }

    @Test
    void rejectsNullChangeCollection() {

        assertThrows(
                NullPointerException.class,
                () -> createAssessment(
                        ManifestFreshnessStatus.CURRENT,
                        null,
                        null
                )
        );
    }

    @Test
    void rejectsNullAssessmentTimestamp() {

        assertThrows(
                NullPointerException.class,
                () -> new ManifestFreshnessAssessment(
                        manifestId,
                        ManifestFreshnessStatus.CURRENT,
                        Set.of(),
                        null,
                        null
                )
        );
    }
}