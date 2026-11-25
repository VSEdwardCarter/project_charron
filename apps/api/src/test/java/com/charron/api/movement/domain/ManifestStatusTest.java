package com.charron.api.movement.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ManifestStatusTest {

    @Test
    void draftCanOnlyTransitionToReviewed() {

        assertEquals(
                Set.of(ManifestStatus.REVIEWED),
                ManifestStatus.DRAFT.allowedTransitions()
        );
    }

    @Test
    void reviewedCanOnlyTransitionToIssued() {

        assertEquals(
                Set.of(ManifestStatus.ISSUED),
                ManifestStatus.REVIEWED.allowedTransitions()
        );
    }

    @Test
    void issuedCanOnlyTransitionToSuperseded() {

        assertEquals(
                Set.of(ManifestStatus.SUPERSEDED),
                ManifestStatus.ISSUED.allowedTransitions()
        );
    }

    @Test
    void supersededHasNoAllowedTransitions() {

        assertTrue(
                ManifestStatus.SUPERSEDED
                        .allowedTransitions()
                        .isEmpty()
        );
    }

    @Test
    void permitsForwardLifecycleTransitions() {

        assertTrue(
                ManifestStatus.DRAFT.canTransitionTo(
                        ManifestStatus.REVIEWED
                )
        );

        assertTrue(
                ManifestStatus.REVIEWED.canTransitionTo(
                        ManifestStatus.ISSUED
                )
        );

        assertTrue(
                ManifestStatus.ISSUED.canTransitionTo(
                        ManifestStatus.SUPERSEDED
                )
        );
    }

    @Test
    void rejectsSkippingLifecycleStates() {

        assertFalse(
                ManifestStatus.DRAFT.canTransitionTo(
                        ManifestStatus.ISSUED
                )
        );

        assertFalse(
                ManifestStatus.DRAFT.canTransitionTo(
                        ManifestStatus.SUPERSEDED
                )
        );

        assertFalse(
                ManifestStatus.REVIEWED.canTransitionTo(
                        ManifestStatus.SUPERSEDED
                )
        );
    }

    @Test
    void rejectsBackwardLifecycleTransitions() {

        assertFalse(
                ManifestStatus.REVIEWED.canTransitionTo(
                        ManifestStatus.DRAFT
                )
        );

        assertFalse(
                ManifestStatus.ISSUED.canTransitionTo(
                        ManifestStatus.REVIEWED
                )
        );

        assertFalse(
                ManifestStatus.ISSUED.canTransitionTo(
                        ManifestStatus.DRAFT
                )
        );

        for (ManifestStatus target : ManifestStatus.values()) {

            assertFalse(
                    ManifestStatus.SUPERSEDED
                            .canTransitionTo(target)
            );
        }
    }

    @Test
    void rejectsTransitionToSameStatus() {

        for (ManifestStatus status : ManifestStatus.values()) {

            assertFalse(status.canTransitionTo(status));
        }
    }

    @Test
    void rejectsNullTransitionTarget() {

        for (ManifestStatus status : ManifestStatus.values()) {

            assertFalse(status.canTransitionTo(null));
        }
    }

    @Test
    void onlySupersededIsTerminal() {

        for (ManifestStatus status : ManifestStatus.values()) {

            assertEquals(
                    status == ManifestStatus.SUPERSEDED,
                    status.isTerminal()
            );
        }
    }

    @Test
    void onlyIssuedRepresentsCurrentlyIssuedStatus() {

        for (ManifestStatus status : ManifestStatus.values()) {

            assertEquals(
                    status == ManifestStatus.ISSUED,
                    status.isIssued()
            );
        }
    }
}