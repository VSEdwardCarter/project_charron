package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestLifecycle;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ManifestIssuanceRollbackTest {

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

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Movement movement;

    private final UUID reviewerId = UUID.randomUUID();
    private final UUID issuerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        movement = Movement.plan(
                "TEST-ISSUANCE-ROLLBACK-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);
    }

    private TransactionTemplate nestedTransaction() {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setPropagationBehavior(
                Propagation.NESTED.value()
        );

        return transaction;
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

    private ManifestStatus status(UUID manifestId) {

        return lifecycleService
                .getLifecycle(manifestId)
                .status();
    }

    @Test
    void rollbackRestoresPreviouslyIssuedManifest() {

        MovementManifest original =
                generateIssuedManifest();

        MovementManifest replacement =
                generateReviewedManifest();

        nestedTransaction().executeWithoutResult(transaction -> {

            lifecycleService.issue(
                    replacement.id(),
                    issuerId,
                    "Replacement issued"
            );

            assertEquals(
                    ManifestStatus.SUPERSEDED,
                    status(original.id())
            );

            assertEquals(
                    ManifestStatus.ISSUED,
                    status(replacement.id())
            );

            transaction.setRollbackOnly();
        });

        assertEquals(
                ManifestStatus.ISSUED,
                status(original.id())
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                status(replacement.id())
        );
    }

    @Test
    void rollbackRemovesBothLifecycleEvents() {

        MovementManifest original =
                generateIssuedManifest();

        MovementManifest replacement =
                generateReviewedManifest();

        var originalEvents =
                eventRepository.findByManifestId(
                        original.id()
                );

        var replacementEvents =
                eventRepository.findByManifestId(
                        replacement.id()
                );

        nestedTransaction().executeWithoutResult(transaction -> {

            lifecycleService.issue(
                    replacement.id(),
                    issuerId,
                    "Replacement issued"
            );

            assertEquals(
                    3,
                    eventRepository.findByManifestId(
                            original.id()
                    ).size()
            );

            assertEquals(
                    2,
                    eventRepository.findByManifestId(
                            replacement.id()
                    ).size()
            );

            transaction.setRollbackOnly();
        });

        assertEquals(
                originalEvents,
                eventRepository.findByManifestId(
                        original.id()
                )
        );

        assertEquals(
                replacementEvents,
                eventRepository.findByManifestId(
                        replacement.id()
                )
        );
    }

    @Test
    void permitsSuccessfulIssuanceAfterRollback() {

        MovementManifest original =
                generateIssuedManifest();

        MovementManifest replacement =
                generateReviewedManifest();

        nestedTransaction().executeWithoutResult(transaction -> {

            lifecycleService.issue(
                    replacement.id(),
                    issuerId,
                    "Rolled back issuance"
            );

            transaction.setRollbackOnly();
        });

        ManifestLifecycle issued =
                lifecycleService.issue(
                        replacement.id(),
                        issuerId,
                        "Successful replacement"
                );

        assertEquals(
                ManifestStatus.ISSUED,
                issued.status()
        );

        assertEquals(
                ManifestStatus.SUPERSEDED,
                status(original.id())
        );

        assertEquals(
                3,
                eventRepository.findByManifestId(
                        original.id()
                ).size()
        );

        assertEquals(
                2,
                eventRepository.findByManifestId(
                        replacement.id()
                ).size()
        );
    }

    @Test
    void rollbackPreservesExactlyOneIssuedManifest() {

        MovementManifest original =
                generateIssuedManifest();

        MovementManifest replacement =
                generateReviewedManifest();

        nestedTransaction().executeWithoutResult(transaction -> {

            lifecycleService.issue(
                    replacement.id(),
                    issuerId,
                    null
            );

            transaction.setRollbackOnly();
        });

        List<MovementManifest> issued =
                manifestRepository.findByMovementId(
                                movement.id()
                        )
                        .stream()
                        .filter(manifest ->
                                status(manifest.id())
                                        == ManifestStatus.ISSUED
                        )
                        .toList();

        assertEquals(
                List.of(original),
                issued
        );
    }

    @Test
    void rollbackPreservesImmutableManifestSnapshots() {

        MovementManifest original =
                generateIssuedManifest();

        MovementManifest replacement =
                generateReviewedManifest();

        nestedTransaction().executeWithoutResult(transaction -> {

            lifecycleService.issue(
                    replacement.id(),
                    issuerId,
                    null
            );

            transaction.setRollbackOnly();
        });

        assertEquals(
                original,
                manifestRepository.findById(
                        original.id()
                ).orElseThrow()
        );

        assertEquals(
                replacement,
                manifestRepository.findById(
                        replacement.id()
                ).orElseThrow()
        );

        assertEquals(
                2,
                manifestRepository.findLatestVersion(
                        movement.id()
                )
        );
    }

    @Test
    void rollbackOfInitialIssuanceRestoresReviewedStatus() {

        MovementManifest manifest =
                generateReviewedManifest();

        nestedTransaction().executeWithoutResult(transaction -> {

            lifecycleService.issue(
                    manifest.id(),
                    issuerId,
                    null
            );

            assertEquals(
                    ManifestStatus.ISSUED,
                    status(manifest.id())
            );

            transaction.setRollbackOnly();
        });

        assertEquals(
                ManifestStatus.REVIEWED,
                status(manifest.id())
        );

        assertEquals(
                1,
                eventRepository.findByManifestId(
                        manifest.id()
                ).size()
        );
    }
}