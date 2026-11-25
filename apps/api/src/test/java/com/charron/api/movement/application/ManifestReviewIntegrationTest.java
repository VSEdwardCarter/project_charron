package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestLifecycle;
import com.charron.api.movement.domain.ManifestLifecycleEvent;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ManifestReviewIntegrationTest {

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private ManifestLifecycleService lifecycleService;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    @Autowired
    private ManifestLifecycleEventRepository eventRepository;

    private Movement movement;
    private MovementManifest manifest;

    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        movement = Movement.plan(
                "TEST-REVIEW-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        manifest = manifestService.generateAndPersist(
                movement.id()
        );
    }

    @Test
    void generatedManifestInitiallyHasDraftStatus() {

        ManifestLifecycle lifecycle =
                lifecycleService.getLifecycle(manifest.id());

        assertEquals(
                ManifestStatus.DRAFT,
                lifecycle.status()
        );

        assertTrue(lifecycle.events().isEmpty());
    }

    @Test
    void reviewsPersistedManifest() {

        ManifestLifecycle reviewed =
                lifecycleService.review(
                        manifest.id(),
                        actorId,
                        "Manifest verified"
                );

        assertEquals(
                ManifestStatus.REVIEWED,
                reviewed.status()
        );

        assertEquals(1, reviewed.events().size());
    }

    @Test
    void persistsCompleteReviewEvent() {

        ManifestLifecycle reviewed =
                lifecycleService.review(
                        manifest.id(),
                        actorId,
                        "Cargo and equipment verified"
                );

        ManifestLifecycleEvent expected =
                reviewed.events().getFirst();

        ManifestLifecycleEvent persisted =
                eventRepository.findById(expected.id())
                        .orElseThrow();

        assertEquals(expected, persisted);
        assertEquals(actorId, persisted.actorId());
        assertEquals(manifest.id(), persisted.manifestId());

        assertEquals(
                ManifestStatus.DRAFT,
                persisted.previousStatus()
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                persisted.newStatus()
        );

        assertEquals(
                "Cargo and equipment verified",
                persisted.remarks()
        );
    }

    @Test
    void reconstructsReviewedStatusFromDatabase() {

        ManifestLifecycle reviewed =
                lifecycleService.review(
                        manifest.id(),
                        actorId,
                        null
                );

        ManifestLifecycle restored =
                lifecycleService.getLifecycle(manifest.id());

        assertEquals(
                ManifestStatus.REVIEWED,
                restored.status()
        );

        assertEquals(
                reviewed.events(),
                restored.events()
        );

        assertEquals(
                reviewed.lastTransitionAt(),
                restored.lastTransitionAt()
        );
    }

    @Test
    void reviewPreservesOriginalManifestSnapshot() {

        lifecycleService.review(
                manifest.id(),
                actorId,
                "Verified"
        );

        MovementManifest restored =
                manifestRepository.findById(manifest.id())
                        .orElseThrow();

        assertEquals(manifest, restored);
    }

    @Test
    void reviewDoesNotConsumeManifestVersion() {

        lifecycleService.review(
                manifest.id(),
                actorId,
                null
        );

        assertEquals(
                1,
                manifestRepository.findLatestVersion(
                        movement.id()
                )
        );

        assertEquals(
                1,
                manifestRepository.findByMovementId(
                        movement.id()
                ).size()
        );
    }

    @Test
    void rejectsRepeatedReview() {

        lifecycleService.review(
                manifest.id(),
                actorId,
                null
        );

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> lifecycleService.review(
                        manifest.id(),
                        actorId,
                        "Repeated review"
                )
        );
    }

    @Test
    void maintainsIndependentLifecycleForEachVersion() {

        MovementManifest second =
                manifestService.generateAndPersist(
                        movement.id()
                );

        lifecycleService.review(
                manifest.id(),
                actorId,
                null
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                lifecycleService.getLifecycle(
                        manifest.id()
                ).status()
        );

        assertEquals(
                ManifestStatus.DRAFT,
                lifecycleService.getLifecycle(
                        second.id()
                ).status()
        );

        assertTrue(
                eventRepository.findByManifestId(
                        second.id()
                ).isEmpty()
        );
    }

    @Test
    void reviewDoesNotChangeMovementStatus() {

        lifecycleService.review(
                manifest.id(),
                actorId,
                null
        );

        Movement restored =
                movementRepository.findById(movement.id())
                        .orElseThrow();

        assertEquals(movement, restored);
    }

    @Test
    void preservesOptionalReviewRemarks() {

        lifecycleService.review(
                manifest.id(),
                actorId,
                null
        );

        List<ManifestLifecycleEvent> events =
                eventRepository.findByManifestId(
                        manifest.id()
                );

        assertEquals(1, events.size());
        assertNull(events.getFirst().remarks());
    }
}