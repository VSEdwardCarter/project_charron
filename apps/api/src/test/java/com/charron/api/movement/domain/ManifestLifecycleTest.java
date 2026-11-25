package com.charron.api.movement.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ManifestLifecycleTest {

    private MovementManifest manifest;

    private final UUID actorId = UUID.randomUUID();

    private final Instant generatedAt =
            Instant.parse("2026-11-24T15:00:00Z");

    @BeforeEach
    void setUp() {

        manifest = new MovementManifest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "TEST-MANIFEST-001",
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z"),
                List.of(),
                List.of(),
                List.of(),
                generatedAt
        );
    }

    private ManifestLifecycleEvent event(
            ManifestStatus previous,
            ManifestStatus next,
            long secondsAfterGeneration
    ) {

        return new ManifestLifecycleEvent(
                UUID.randomUUID(),
                manifest.id(),
                previous,
                next,
                actorId,
                generatedAt.plusSeconds(secondsAfterGeneration),
                null
        );
    }

    private ManifestLifecycleEvent review() {

        return event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                10
        );
    }

    private ManifestLifecycleEvent issue() {

        return event(
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED,
                20
        );
    }

    private ManifestLifecycleEvent supersede() {

        return event(
                ManifestStatus.ISSUED,
                ManifestStatus.SUPERSEDED,
                30
        );
    }

    @Test
    void reconstructsDraftWithoutEvents() {

        ManifestLifecycle lifecycle =
                ManifestLifecycle.reconstruct(
                        manifest,
                        List.of()
                );

        assertEquals(manifest.id(), lifecycle.manifestId());
        assertEquals(ManifestStatus.DRAFT, lifecycle.status());
        assertEquals(generatedAt, lifecycle.generatedAt());
        assertEquals(generatedAt, lifecycle.lastTransitionAt());

        assertTrue(lifecycle.events().isEmpty());
        assertFalse(lifecycle.isIssued());
        assertFalse(lifecycle.isTerminal());
    }

    @Test
    void reconstructsReviewedManifest() {

        ManifestLifecycleEvent review = review();

        ManifestLifecycle lifecycle =
                ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(review)
                );

        assertEquals(
                ManifestStatus.REVIEWED,
                lifecycle.status()
        );

        assertEquals(
                review.recordedAt(),
                lifecycle.lastTransitionAt()
        );
    }

    @Test
    void reconstructsIssuedManifest() {

        ManifestLifecycle lifecycle =
                ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(review(), issue())
                );

        assertEquals(
                ManifestStatus.ISSUED,
                lifecycle.status()
        );

        assertTrue(lifecycle.isIssued());
        assertFalse(lifecycle.isTerminal());
    }

    @Test
    void reconstructsSupersededManifest() {

        ManifestLifecycle lifecycle =
                ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(
                                review(),
                                issue(),
                                supersede()
                        )
                );

        assertEquals(
                ManifestStatus.SUPERSEDED,
                lifecycle.status()
        );

        assertFalse(lifecycle.isIssued());
        assertTrue(lifecycle.isTerminal());
    }

    @Test
    void reconstructsEventsInChronologicalOrder() {

        ManifestLifecycleEvent review = review();
        ManifestLifecycleEvent issue = issue();
        ManifestLifecycleEvent supersede = supersede();

        ManifestLifecycle lifecycle =
                ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(supersede, review, issue)
                );

        assertEquals(
                ManifestStatus.SUPERSEDED,
                lifecycle.status()
        );

        assertEquals(
                List.of(review, issue, supersede),
                lifecycle.events()
        );
    }

    @Test
    void reconstructsEventsWithIdenticalTimestamps() {

        ManifestLifecycleEvent review = event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                10
        );

        ManifestLifecycleEvent issue = event(
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED,
                10
        );

        ManifestLifecycle lifecycle =
                ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(issue, review)
                );

        assertEquals(
                ManifestStatus.ISSUED,
                lifecycle.status()
        );

        assertEquals(
                List.of(review, issue),
                lifecycle.events()
        );
    }

    @Test
    void rejectsIssuanceWithoutReview() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(issue())
                )
        );
    }

    @Test
    void rejectsSupersessionWithoutIssuance() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(review(), supersede())
                )
        );
    }

    @Test
    void rejectsDuplicateEventIdentity() {

        ManifestLifecycleEvent review = review();

        assertThrows(
                IllegalArgumentException.class,
                () -> ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(review, review)
                )
        );
    }

    @Test
    void rejectsRepeatedLifecycleTransition() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(review(), review())
                )
        );
    }

    @Test
    void rejectsEventBelongingToAnotherManifest() {

        ManifestLifecycleEvent foreignEvent =
                new ManifestLifecycleEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        actorId,
                        generatedAt.plusSeconds(10),
                        null
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(foreignEvent)
                )
        );
    }

    @Test
    void rejectsEventBeforeManifestGeneration() {

        ManifestLifecycleEvent invalidEvent = event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                -10
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(invalidEvent)
                )
        );
    }

    @Test
    void rejectsIncorrectChronologicalSequence() {

        ManifestLifecycleEvent review = event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                20
        );

        ManifestLifecycleEvent issue = event(
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED,
                10
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(review, issue)
                )
        );
    }

    @Test
    void preservesImmutableEventCollection() {

        ManifestLifecycle lifecycle =
                ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(review())
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> lifecycle.events().add(issue())
        );
    }

    @Test
    void protectsAgainstModificationOfSourceCollection() {

        List<ManifestLifecycleEvent> source =
                new ArrayList<>();

        source.add(review());

        ManifestLifecycle lifecycle =
                ManifestLifecycle.reconstruct(
                        manifest,
                        source
                );

        source.clear();

        assertEquals(1, lifecycle.events().size());

        assertEquals(
                ManifestStatus.REVIEWED,
                lifecycle.status()
        );
    }

    @Test
    void determinesPermittedNextTransition() {

        ManifestLifecycle lifecycle =
                ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(review())
                );

        assertTrue(
                lifecycle.canTransitionTo(
                        ManifestStatus.ISSUED
                )
        );

        assertFalse(
                lifecycle.canTransitionTo(
                        ManifestStatus.DRAFT
                )
        );

        assertFalse(
                lifecycle.canTransitionTo(
                        ManifestStatus.SUPERSEDED
                )
        );
    }

    @Test
    void returnsMostRecentTransitionTimestamp() {

        ManifestLifecycleEvent review = review();
        ManifestLifecycleEvent issue = issue();

        ManifestLifecycle lifecycle =
                ManifestLifecycle.reconstruct(
                        manifest,
                        List.of(issue, review)
                );

        assertEquals(
                issue.recordedAt(),
                lifecycle.lastTransitionAt()
        );
    }

    @Test
    void rejectsNullManifest() {

        assertThrows(
                NullPointerException.class,
                () -> ManifestLifecycle.reconstruct(
                        null,
                        List.of()
                )
        );
    }

    @Test
    void rejectsNullEventCollection() {

        assertThrows(
                NullPointerException.class,
                () -> ManifestLifecycle.reconstruct(
                        manifest,
                        null
                )
        );
    }

    @Test
    void rejectsNullEvent() {

        List<ManifestLifecycleEvent> events =
                new ArrayList<>();

        events.add(null);

        assertThrows(
                NullPointerException.class,
                () -> ManifestLifecycle.reconstruct(
                        manifest,
                        events
                )
        );
    }
}