package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestLifecycle;
import com.charron.api.movement.domain.ManifestLifecycleEvent;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManifestLifecycleServiceTest {

    private MovementManifestRepository manifestRepository;
    private ManifestLifecycleEventRepository eventRepository;

    private ManifestLifecycleService service;

    private MovementManifest manifest;

    private final UUID actorId = UUID.randomUUID();

    private final Instant generatedAt =
            Instant.parse("2026-11-24T15:00:00Z");

    @BeforeEach
    void setUp() {

        manifestRepository =
                mock(MovementManifestRepository.class);

        eventRepository =
                mock(ManifestLifecycleEventRepository.class);

        service = new ManifestLifecycleService(
                manifestRepository,
                eventRepository,
                mock(MovementRepository.class),
                mock(MovementManifestService.class)
        );

        manifest = new MovementManifest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "TEST-LIFECYCLE-SERVICE",
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z"),
                List.of(),
                List.of(),
                List.of(),
                generatedAt
        );

        when(manifestRepository.findById(manifest.id()))
                .thenReturn(Optional.of(manifest));

        when(eventRepository.findByManifestId(manifest.id()))
                .thenReturn(List.of());
    }

    private ManifestLifecycleEvent event(
            ManifestStatus previous,
            ManifestStatus next,
            long seconds
    ) {

        return new ManifestLifecycleEvent(
                UUID.randomUUID(),
                manifest.id(),
                previous,
                next,
                actorId,
                generatedAt.plusSeconds(seconds),
                null
        );
    }

    @Test
    void retrievesDraftLifecycle() {

        ManifestLifecycle lifecycle =
                service.getLifecycle(manifest.id());

        assertEquals(manifest.id(), lifecycle.manifestId());
        assertEquals(ManifestStatus.DRAFT, lifecycle.status());
        assertTrue(lifecycle.events().isEmpty());
        assertEquals(generatedAt, lifecycle.generatedAt());

        verify(manifestRepository).findById(manifest.id());
        verify(eventRepository).findByManifestId(manifest.id());
    }

    @Test
    void retrievesReviewedLifecycle() {

        ManifestLifecycleEvent review = event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                10
        );

        when(eventRepository.findByManifestId(manifest.id()))
                .thenReturn(List.of(review));

        ManifestLifecycle lifecycle =
                service.getLifecycle(manifest.id());

        assertEquals(
                ManifestStatus.REVIEWED,
                lifecycle.status()
        );

        assertEquals(List.of(review), lifecycle.events());
    }

    @Test
    void retrievesIssuedLifecycle() {

        ManifestLifecycleEvent review = event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                10
        );

        ManifestLifecycleEvent issue = event(
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED,
                20
        );

        when(eventRepository.findByManifestId(manifest.id()))
                .thenReturn(List.of(review, issue));

        ManifestLifecycle lifecycle =
                service.getLifecycle(manifest.id());

        assertEquals(
                ManifestStatus.ISSUED,
                lifecycle.status()
        );

        assertTrue(lifecycle.isIssued());

        assertEquals(
                issue.recordedAt(),
                lifecycle.lastTransitionAt()
        );
    }

    @Test
    void retrievesSupersededLifecycle() {

        ManifestLifecycleEvent review = event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                10
        );

        ManifestLifecycleEvent issue = event(
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED,
                20
        );

        ManifestLifecycleEvent supersede = event(
                ManifestStatus.ISSUED,
                ManifestStatus.SUPERSEDED,
                30
        );

        when(eventRepository.findByManifestId(manifest.id()))
                .thenReturn(
                        List.of(review, issue, supersede)
                );

        ManifestLifecycle lifecycle =
                service.getLifecycle(manifest.id());

        assertEquals(
                ManifestStatus.SUPERSEDED,
                lifecycle.status()
        );

        assertTrue(lifecycle.isTerminal());
        assertFalse(lifecycle.isIssued());
        assertEquals(3, lifecycle.events().size());
    }

    @Test
    void rejectsUnknownManifest() {

        UUID unknownId = UUID.randomUUID();

        when(manifestRepository.findById(unknownId))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getLifecycle(unknownId)
        );

        assertTrue(
                exception.getMessage()
                        .contains("Manifest not found")
        );

        verifyNoInteractions(eventRepository);
    }

    @Test
    void rejectsNullManifestIdentity() {

        assertThrows(
                NullPointerException.class,
                () -> service.getLifecycle(null)
        );

        verifyNoInteractions(manifestRepository);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void rejectsInconsistentLifecycleHistory() {

        ManifestLifecycleEvent issue = event(
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED,
                20
        );

        when(eventRepository.findByManifestId(manifest.id()))
                .thenReturn(List.of(issue));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getLifecycle(manifest.id())
        );
    }

    @Test
    void retrievesLifecycleWithoutModifyingRecords() {

        service.getLifecycle(manifest.id());

        verify(manifestRepository, never())
                .save(any(MovementManifest.class));

        verify(eventRepository, never())
                .save(any(ManifestLifecycleEvent.class));
    }
}