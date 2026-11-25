package com.charron.api.movement.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ManifestLifecycleEventTest {

    private final UUID eventId = UUID.randomUUID();
    private final UUID manifestId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    private final Instant recordedAt =
            Instant.parse("2026-11-24T15:00:00Z");

    private ManifestLifecycleEvent createEvent(
            ManifestStatus previous,
            ManifestStatus next,
            String remarks
    ) {
        return new ManifestLifecycleEvent(
                eventId,
                manifestId,
                previous,
                next,
                actorId,
                recordedAt,
                remarks
        );
    }

    @Test
    void preservesCompleteEventDetails() {

        ManifestLifecycleEvent event = createEvent(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                "Manifest reviewed successfully"
        );

        assertEquals(eventId, event.id());
        assertEquals(manifestId, event.manifestId());
        assertEquals(actorId, event.actorId());
        assertEquals(recordedAt, event.recordedAt());

        assertEquals(
                ManifestStatus.DRAFT,
                event.previousStatus()
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                event.newStatus()
        );

        assertEquals(
                "Manifest reviewed successfully",
                event.remarks()
        );
    }

    @Test
    void permitsReviewTransition() {

        assertDoesNotThrow(() -> createEvent(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                null
        ));
    }

    @Test
    void permitsIssuanceTransition() {

        assertDoesNotThrow(() -> createEvent(
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED,
                null
        ));
    }

    @Test
    void permitsSupersessionTransition() {

        assertDoesNotThrow(() -> createEvent(
                ManifestStatus.ISSUED,
                ManifestStatus.SUPERSEDED,
                null
        ));
    }

    @Test
    void rejectsSkippedLifecycleTransition() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createEvent(
                        ManifestStatus.DRAFT,
                        ManifestStatus.ISSUED,
                        null
                )
        );
    }

    @Test
    void rejectsBackwardTransition() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createEvent(
                        ManifestStatus.ISSUED,
                        ManifestStatus.DRAFT,
                        null
                )
        );
    }

    @Test
    void rejectsSameStatusTransition() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createEvent(
                        ManifestStatus.REVIEWED,
                        ManifestStatus.REVIEWED,
                        null
                )
        );
    }

    @Test
    void rejectsTransitionFromSuperseded() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createEvent(
                        ManifestStatus.SUPERSEDED,
                        ManifestStatus.ISSUED,
                        null
                )
        );
    }

    @Test
    void rejectsMissingEventIdentity() {

        assertThrows(
                NullPointerException.class,
                () -> new ManifestLifecycleEvent(
                        null,
                        manifestId,
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        actorId,
                        recordedAt,
                        null
                )
        );
    }

    @Test
    void rejectsMissingManifestIdentity() {

        assertThrows(
                NullPointerException.class,
                () -> new ManifestLifecycleEvent(
                        eventId,
                        null,
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        actorId,
                        recordedAt,
                        null
                )
        );
    }

    @Test
    void rejectsMissingActorIdentity() {

        assertThrows(
                NullPointerException.class,
                () -> new ManifestLifecycleEvent(
                        eventId,
                        manifestId,
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        null,
                        recordedAt,
                        null
                )
        );
    }

    @Test
    void rejectsMissingTimestamp() {

        assertThrows(
                NullPointerException.class,
                () -> new ManifestLifecycleEvent(
                        eventId,
                        manifestId,
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        actorId,
                        null,
                        null
                )
        );
    }

    @Test
    void rejectsMissingPreviousStatus() {

        assertThrows(
                NullPointerException.class,
                () -> createEvent(
                        null,
                        ManifestStatus.REVIEWED,
                        null
                )
        );
    }

    @Test
    void rejectsMissingNewStatus() {

        assertThrows(
                NullPointerException.class,
                () -> createEvent(
                        ManifestStatus.DRAFT,
                        null,
                        null
                )
        );
    }

    @Test
    void permitsNullRemarks() {

        ManifestLifecycleEvent event = createEvent(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                null
        );

        assertNull(event.remarks());
    }

    @Test
    void normalizesBlankRemarks() {

        ManifestLifecycleEvent event = createEvent(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                "   "
        );

        assertNull(event.remarks());
    }

    @Test
    void trimsRemarks() {

        ManifestLifecycleEvent event = createEvent(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                "  Cargo verified  "
        );

        assertEquals(
                "Cargo verified",
                event.remarks()
        );
    }
}