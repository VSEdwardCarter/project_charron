package com.charron.api.movement.application;

import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MovementManifestRollbackTest {

    @Autowired
    private MovementManifestService service;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Movement createMovement() {

        return Movement.plan(
                "TEST-MANIFEST-ROLLBACK-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );
    }

    private TransactionTemplate nestedTransaction() {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setPropagationBehavior(
                Propagation.NESTED.value()
        );

        return transaction;
    }

    @Test
    void rolledBackManifestDoesNotRemainPersisted() {

        Movement movement = createMovement();

        movementRepository.save(movement);

        MovementManifest rolledBack =
                nestedTransaction().execute(status -> {

                    MovementManifest manifest =
                            service.generateAndPersist(
                                    movement.id()
                            );

                    assertTrue(
                            manifestRepository.findById(
                                    manifest.id()
                            ).isPresent()
                    );

                    status.setRollbackOnly();

                    return manifest;
                });

        assertNotNull(rolledBack);

        assertTrue(
                manifestRepository.findById(
                        rolledBack.id()
                ).isEmpty()
        );

        assertEquals(
                0,
                manifestRepository.findLatestVersion(
                        movement.id()
                )
        );
    }

    @Test
    void rollbackDoesNotConsumeFirstManifestVersion() {

        Movement movement = createMovement();

        movementRepository.save(movement);

        nestedTransaction().executeWithoutResult(status -> {

            MovementManifest manifest =
                    service.generateAndPersist(
                            movement.id()
                    );

            assertEquals(1, manifest.version());

            status.setRollbackOnly();
        });

        MovementManifest persisted =
                service.generateAndPersist(movement.id());

        assertEquals(1, persisted.version());

        assertEquals(
                1,
                manifestRepository.findByMovementId(
                        movement.id()
                ).size()
        );
    }

    @Test
    void rollbackPreservesExistingManifestHistory() {

        Movement movement = createMovement();

        movementRepository.save(movement);

        MovementManifest first =
                service.generateAndPersist(movement.id());

        nestedTransaction().executeWithoutResult(status -> {

            MovementManifest second =
                    service.generateAndPersist(
                            movement.id()
                    );

            assertEquals(2, second.version());

            status.setRollbackOnly();
        });

        assertEquals(
                first,
                manifestRepository.findLatestByMovementId(
                        movement.id()
                ).orElseThrow()
        );

        assertEquals(
                1,
                manifestRepository.findByMovementId(
                        movement.id()
                ).size()
        );
    }

    @Test
    void reusesNextSequentialVersionAfterRollback() {

        Movement movement = createMovement();

        movementRepository.save(movement);

        service.generateAndPersist(movement.id());

        MovementManifest rolledBack =
                nestedTransaction().execute(status -> {

                    MovementManifest manifest =
                            service.generateAndPersist(
                                    movement.id()
                            );

                    assertEquals(2, manifest.version());

                    status.setRollbackOnly();

                    return manifest;
                });

        assertNotNull(rolledBack);

        MovementManifest replacement =
                service.generateAndPersist(movement.id());

        assertEquals(2, replacement.version());

        assertNotEquals(
                rolledBack.id(),
                replacement.id()
        );

        assertEquals(
                2,
                manifestRepository.findLatestVersion(
                        movement.id()
                )
        );

        assertTrue(
                manifestRepository.findById(
                        rolledBack.id()
                ).isEmpty()
        );
    }
}