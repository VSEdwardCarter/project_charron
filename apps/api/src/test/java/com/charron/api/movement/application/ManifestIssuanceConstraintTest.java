package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestLifecycleEvent;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ManifestIssuanceConstraintTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private ManifestLifecycleEventRepository eventRepository;

    @Autowired
    private ManifestLifecycleService lifecycleService;

    private Movement movement;

    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        movement = createMovement();
    }

    private Movement createMovement() {

        Movement created = Movement.plan(
                "TEST-ISSUE-CONSTRAINT-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(created);

        return created;
    }

    private MovementManifest generateManifest(UUID movementId) {

        return manifestService.generateAndPersist(
                movementId
        );
    }

    private ManifestLifecycleEvent insertTransition(
            MovementManifest manifest,
            ManifestStatus previous,
            ManifestStatus next
    ) {

        ManifestLifecycleEvent event =
                new ManifestLifecycleEvent(
                        UUID.randomUUID(),
                        manifest.id(),
                        previous,
                        next,
                        actorId,
                        Instant.now().truncatedTo(
                                ChronoUnit.MICROS
                        ),
                        "Direct database transition test"
                );

        eventRepository.save(event);

        return event;
    }

    private MovementManifest generateReviewedManifest(
            UUID movementId
    ) {

        MovementManifest manifest =
                generateManifest(movementId);

        insertTransition(
                manifest,
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED
        );

        return manifest;
    }

    private MovementManifest generateIssuedManifest(
            UUID movementId
    ) {

        MovementManifest manifest =
                generateReviewedManifest(movementId);

        insertTransition(
                manifest,
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED
        );

        return manifest;
    }

    private ManifestStatus status(
            MovementManifest manifest
    ) {

        return lifecycleService
                .getLifecycle(manifest.id())
                .status();
    }

    @Test
    void permitsInitialDirectIssuance() {

        MovementManifest manifest =
                generateReviewedManifest(
                        movement.id()
                );

        ManifestLifecycleEvent issued =
                insertTransition(
                        manifest,
                        ManifestStatus.REVIEWED,
                        ManifestStatus.ISSUED
                );

        assertEquals(
                ManifestStatus.ISSUED,
                status(manifest)
        );

        assertTrue(
                eventRepository.findById(
                        issued.id()
                ).isPresent()
        );
    }

    @Test
    void rejectsSecondActiveIssuedManifest() {

        generateIssuedManifest(
                movement.id()
        );

        MovementManifest replacement =
                generateReviewedManifest(
                        movement.id()
                );

        DataIntegrityViolationException exception =
                assertThrows(
                        DataIntegrityViolationException.class,
                        () -> insertTransition(
                                replacement,
                                ManifestStatus.REVIEWED,
                                ManifestStatus.ISSUED
                        )
                );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains(
                                "Another issued manifest already exists"
                        )
        );
    }

    @Test
    void permitsIssuanceAfterPreviousManifestSuperseded() {

        MovementManifest original =
                generateIssuedManifest(
                        movement.id()
                );

        MovementManifest replacement =
                generateReviewedManifest(
                        movement.id()
                );

        insertTransition(
                original,
                ManifestStatus.ISSUED,
                ManifestStatus.SUPERSEDED
        );

        insertTransition(
                replacement,
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED
        );

        assertEquals(
                ManifestStatus.SUPERSEDED,
                status(original)
        );

        assertEquals(
                ManifestStatus.ISSUED,
                status(replacement)
        );
    }

    @Test
    void rejectsOlderVersionAfterNewerVersionIssued() {

        MovementManifest older =
                generateReviewedManifest(
                        movement.id()
                );

        generateIssuedManifest(
                movement.id()
        );

        DataIntegrityViolationException exception =
                assertThrows(
                        DataIntegrityViolationException.class,
                        () -> insertTransition(
                                older,
                                ManifestStatus.REVIEWED,
                                ManifestStatus.ISSUED
                        )
                );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains(
                                "Cannot issue an older manifest version"
                        )
        );
    }

    @Test
    void rejectsOlderVersionEvenAfterNewerVersionSuperseded() {

        MovementManifest older =
                generateReviewedManifest(
                        movement.id()
                );

        MovementManifest newer =
                generateIssuedManifest(
                        movement.id()
                );

        insertTransition(
                newer,
                ManifestStatus.ISSUED,
                ManifestStatus.SUPERSEDED
        );

        DataIntegrityViolationException exception =
                assertThrows(
                        DataIntegrityViolationException.class,
                        () -> insertTransition(
                                older,
                                ManifestStatus.REVIEWED,
                                ManifestStatus.ISSUED
                        )
                );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains(
                                "Cannot issue an older manifest version"
                        )
        );
    }

    @Test
    void permitsIssuedManifestsForIndependentMovements() {

        Movement independentMovement =
                createMovement();

        MovementManifest first =
                generateIssuedManifest(
                        movement.id()
                );

        MovementManifest second =
                generateIssuedManifest(
                        independentMovement.id()
                );

        assertNotEquals(
                first.movementId(),
                second.movementId()
        );

        assertEquals(
                ManifestStatus.ISSUED,
                status(first)
        );

        assertEquals(
                ManifestStatus.ISSUED,
                status(second)
        );
    }

    @Test
    void permitsNewerDraftWhileManifestIssued() {

        MovementManifest issued =
                generateIssuedManifest(
                        movement.id()
                );

        MovementManifest draft =
                generateManifest(
                        movement.id()
                );

        assertEquals(
                ManifestStatus.ISSUED,
                status(issued)
        );

        assertEquals(
                ManifestStatus.DRAFT,
                status(draft)
        );
    }

    @Test
    void permitsNewerReviewWhileManifestIssued() {

        MovementManifest issued =
                generateIssuedManifest(
                        movement.id()
                );

        MovementManifest reviewed =
                generateReviewedManifest(
                        movement.id()
                );

        assertEquals(
                ManifestStatus.ISSUED,
                status(issued)
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                status(reviewed)
        );
    }

    @Test
    void permitsOlderReviewedVersionWhenNewerDraftExists() {

        MovementManifest older =
                generateReviewedManifest(
                        movement.id()
                );

        MovementManifest newer =
                generateManifest(
                        movement.id()
                );

        insertTransition(
                older,
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED
        );

        assertEquals(
                ManifestStatus.ISSUED,
                status(older)
        );

        assertEquals(
                ManifestStatus.DRAFT,
                status(newer)
        );
    }

    @Test
    void rejectsRepeatedDirectIssuance() {

        MovementManifest issued =
                generateIssuedManifest(
                        movement.id()
                );

        DataIntegrityViolationException exception =
                assertThrows(
                        DataIntegrityViolationException.class,
                        () -> insertTransition(
                                issued,
                                ManifestStatus.REVIEWED,
                                ManifestStatus.ISSUED
                        )
                );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains(
                                "Invalid manifest lifecycle sequence"
                        )
        );
    }
}