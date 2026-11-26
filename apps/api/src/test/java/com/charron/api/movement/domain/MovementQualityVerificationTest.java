package com.charron.api.movement.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MovementQualityVerificationTest {

    private final UUID id = UUID.randomUUID();

    private final UUID manifestId = UUID.randomUUID();

    private final UUID reviewerId = UUID.randomUUID();

    private final Instant verifiedAt =
            Instant.parse("2026-11-25T20:00:00Z");

    @Test
    void createsCompletedVerification() {

        var verification = createVerification(
                allChecksCompleted(),
                "Inspection completed"
        );

        assertAll(
                () -> assertEquals(id, verification.id()),
                () -> assertEquals(
                        manifestId,
                        verification.manifestId()
                ),
                () -> assertEquals(
                        reviewerId,
                        verification.reviewerId()
                ),
                () -> assertEquals(
                        verifiedAt,
                        verification.verifiedAt()
                ),
                () -> assertEquals(
                        "Inspection completed",
                        verification.remarks()
                ),
                () -> assertTrue(
                        verification.equipmentAssignmentVerified()
                ),
                () -> assertTrue(
                        verification.equipmentInspectionCompleted()
                ),
                () -> assertTrue(
                        verification.cargoReconciliationVerified()
                ),
                () -> assertTrue(
                        verification.loadingArrangementVerified()
                ),
                () -> assertTrue(
                        verification.cargoSecurementVerified()
                ),
                () -> assertTrue(
                        verification.documentationVerified()
                )
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void rejectsVerificationWhenAnyRequiredCheckIsIncomplete(
            int incompleteCheck
    ) {

        boolean[] checks = allChecksCompleted();

        checks[incompleteCheck] = false;

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> createVerification(checks, null)
        );

        assertEquals(
                "All required quality checks must be completed",
                exception.getMessage()
        );
    }

    @Test
    void rejectsMissingVerificationId() {

        assertThrows(
                NullPointerException.class,
                () -> new MovementQualityVerification(
                        null,
                        manifestId,
                        reviewerId,
                        true, true, true,
                        true, true, true,
                        verifiedAt,
                        null
                )
        );
    }

    @Test
    void rejectsMissingManifestId() {

        assertThrows(
                NullPointerException.class,
                () -> new MovementQualityVerification(
                        id,
                        null,
                        reviewerId,
                        true, true, true,
                        true, true, true,
                        verifiedAt,
                        null
                )
        );
    }

    @Test
    void rejectsMissingReviewerId() {

        assertThrows(
                NullPointerException.class,
                () -> new MovementQualityVerification(
                        id,
                        manifestId,
                        null,
                        true, true, true,
                        true, true, true,
                        verifiedAt,
                        null
                )
        );
    }

    @Test
    void rejectsMissingVerificationTimestamp() {

        assertThrows(
                NullPointerException.class,
                () -> new MovementQualityVerification(
                        id,
                        manifestId,
                        reviewerId,
                        true, true, true,
                        true, true, true,
                        null,
                        null
                )
        );
    }

    @Test
    void acceptsNullRemarks() {

        var verification = createVerification(
                allChecksCompleted(),
                null
        );

        assertNull(verification.remarks());
    }

    @Test
    void normalizesBlankRemarksToNull() {

        var verification = createVerification(
                allChecksCompleted(),
                "   "
        );

        assertNull(verification.remarks());
    }

    @Test
    void trimsRemarks() {

        var verification = createVerification(
                allChecksCompleted(),
                "  Cargo reconciled successfully  "
        );

        assertEquals(
                "Cargo reconciled successfully",
                verification.remarks()
        );
    }

    private boolean[] allChecksCompleted() {

        return new boolean[]{
                true, true, true,
                true, true, true
        };
    }

    private MovementQualityVerification createVerification(
            boolean[] checks,
            String remarks
    ) {

        return new MovementQualityVerification(
                id,
                manifestId,
                reviewerId,
                checks[0],
                checks[1],
                checks[2],
                checks[3],
                checks[4],
                checks[5],
                verifiedAt,
                remarks
        );
    }
}