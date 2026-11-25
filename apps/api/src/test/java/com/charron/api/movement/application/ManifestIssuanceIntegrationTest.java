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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ManifestIssuanceIntegrationTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    @Autowired
    private ManifestLifecycleEventRepository eventRepository;

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private ManifestLifecycleService lifecycleService;

    private Movement movement;

    private final UUID reviewerId = UUID.randomUUID();
    private final UUID issuerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        movement = Movement.plan(
                "TEST-ISSUANCE-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);
    }

    private MovementManifest generateReviewedManifest() {

        MovementManifest manifest =
                manifestService.generateAndPersist(
                        movement.id()
                );

        lifecycleService.review(
                manifest.id(),
                reviewerId,
                "Manifest reviewed"
        );

        return manifest;
    }

    private MovementManifest generateIssuedManifest() {

        MovementManifest manifest =
                generateReviewedManifest();

        lifecycleService.issue(
                manifest.id(),
                issuerId,
                "Manifest issued"
        );

        return manifest;
    }

    @Test
    void issuesReviewedManifest() {

        MovementManifest manifest =
                generateReviewedManifest();

        ManifestLifecycle issued =
                lifecycleService.issue(
                        manifest.id(),
                        issuerId,
                        "Approved for issuance"
                );

        assertEquals(
                ManifestStatus.ISSUED,
                issued.status()
        );

        assertTrue(issued.isIssued());
        assertEquals(2, issued.events().size());
    }

    @Test
    void persistsCompleteIssuanceEvent() {

        MovementManifest manifest =
                generateReviewedManifest();

        ManifestLifecycle issued =
                lifecycleService.issue(
                        manifest.id(),
                        issuerId,
                        "Approved for issuance"
                );

        ManifestLifecycleEvent expected =
                issued.events().getLast();

        ManifestLifecycleEvent persisted =
                eventRepository.findById(expected.id())
                        .orElseThrow();

        assertEquals(expected, persisted);
        assertEquals(issuerId, persisted.actorId());
        assertEquals(manifest.id(), persisted.manifestId());

        assertEquals(
                ManifestStatus.REVIEWED,
                persisted.previousStatus()
        );

        assertEquals(
                ManifestStatus.ISSUED,
                persisted.newStatus()
        );

        assertEquals(
                "Approved for issuance",
                persisted.remarks()
        );
    }

    @Test
    void reconstructsIssuedLifecycleFromDatabase() {

        MovementManifest manifest =
                generateIssuedManifest();

        ManifestLifecycle restored =
                lifecycleService.getLifecycle(
                        manifest.id()
                );

        assertEquals(
                ManifestStatus.ISSUED,
                restored.status()
        );

        assertEquals(2, restored.events().size());

        assertEquals(
                reviewerId,
                restored.events().getFirst().actorId()
        );

        assertEquals(
                issuerId,
                restored.events().getLast().actorId()
        );
    }

    @Test
    void automaticallySupersedesPreviouslyIssuedManifest() {

        MovementManifest first =
                generateIssuedManifest();

        MovementManifest second =
                generateIssuedManifest();

        assertEquals(
                ManifestStatus.SUPERSEDED,
                lifecycleService.getLifecycle(
                        first.id()
                ).status()
        );

        assertEquals(
                ManifestStatus.ISSUED,
                lifecycleService.getLifecycle(
                        second.id()
                ).status()
        );
    }

    @Test
    void recordsMatchingSupersessionAndIssuanceTimestamps() {

        MovementManifest first =
                generateIssuedManifest();

        MovementManifest second =
                generateIssuedManifest();

        ManifestLifecycleEvent supersession =
                lifecycleService.getLifecycle(first.id())
                        .events()
                        .getLast();

        ManifestLifecycleEvent issuance =
                lifecycleService.getLifecycle(second.id())
                        .events()
                        .getLast();

        assertEquals(
                ManifestStatus.SUPERSEDED,
                supersession.newStatus()
        );

        assertEquals(
                ManifestStatus.ISSUED,
                issuance.newStatus()
        );

        assertEquals(
                supersession.recordedAt(),
                issuance.recordedAt()
        );

        assertTrue(
                supersession.remarks().contains(
                        second.id().toString()
                )
        );
    }

    @Test
    void maintainsOnlyOneActiveIssuedManifest() {

        MovementManifest first =
                generateIssuedManifest();

        MovementManifest second =
                generateIssuedManifest();

        MovementManifest third =
                generateIssuedManifest();

        long issuedCount = manifestRepository
                .findByMovementId(movement.id())
                .stream()
                .map(item -> lifecycleService.getLifecycle(
                        item.id()
                ))
                .filter(ManifestLifecycle::isIssued)
                .count();

        assertEquals(1L, issuedCount);

        assertEquals(
                ManifestStatus.SUPERSEDED,
                lifecycleService.getLifecycle(
                        first.id()
                ).status()
        );

        assertEquals(
                ManifestStatus.SUPERSEDED,
                lifecycleService.getLifecycle(
                        second.id()
                ).status()
        );

        assertEquals(
                ManifestStatus.ISSUED,
                lifecycleService.getLifecycle(
                        third.id()
                ).status()
        );
    }

    @Test
    void newerDraftDoesNotSupersedeIssuedManifest() {

        MovementManifest issued =
                generateIssuedManifest();

        MovementManifest draft =
                manifestService.generateAndPersist(
                        movement.id()
                );

        assertEquals(
                ManifestStatus.ISSUED,
                lifecycleService.getLifecycle(
                        issued.id()
                ).status()
        );

        assertEquals(
                ManifestStatus.DRAFT,
                lifecycleService.getLifecycle(
                        draft.id()
                ).status()
        );
    }

    @Test
    void newerReviewedManifestDoesNotSupersedeIssuedManifest() {

        MovementManifest issued =
                generateIssuedManifest();

        MovementManifest reviewed =
                generateReviewedManifest();

        assertEquals(
                ManifestStatus.ISSUED,
                lifecycleService.getLifecycle(
                        issued.id()
                ).status()
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                lifecycleService.getLifecycle(
                        reviewed.id()
                ).status()
        );
    }

    @Test
    void preservesOriginalManifestSnapshots() {

        MovementManifest first =
                generateIssuedManifest();

        MovementManifest second =
                generateIssuedManifest();

        assertEquals(
                first,
                manifestRepository.findById(first.id())
                        .orElseThrow()
        );

        assertEquals(
                second,
                manifestRepository.findById(second.id())
                        .orElseThrow()
        );
    }

    @Test
    void issuanceDoesNotConsumeManifestVersion() {

        generateIssuedManifest();

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
    void issuanceDoesNotChangeMovementStatus() {

        generateIssuedManifest();

        Movement restored =
                movementRepository.findById(
                        movement.id()
                ).orElseThrow();

        assertEquals(movement, restored);
    }

    @Test
    void rejectsIssuanceWithoutReview() {

        MovementManifest draft =
                manifestService.generateAndPersist(
                        movement.id()
                );

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> lifecycleService.issue(
                        draft.id(),
                        issuerId,
                        null
                )
        );
    }

    @Test
    void rejectsIssuanceOfOlderReviewedVersion() {

        MovementManifest older =
                generateReviewedManifest();

        generateIssuedManifest();

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> lifecycleService.issue(
                        older.id(),
                        issuerId,
                        null
                )
        );
    }

    @Test
    void rejectsRepeatedIssuance() {

        MovementManifest issued =
                generateIssuedManifest();

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> lifecycleService.issue(
                        issued.id(),
                        issuerId,
                        null
                )
        );
    }
}