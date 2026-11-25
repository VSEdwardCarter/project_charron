package com.charron.api.movement.application;

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
class MovementManifestPersistenceIntegrationTest {

    @Autowired
    private MovementManifestService service;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    private Movement movement;

    @BeforeEach
    void setUp() {

        movement = createMovement();

        movementRepository.save(movement);
    }

    private Movement createMovement() {

        return Movement.plan(
                "TEST-MANIFEST-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );
    }

    @Test
    void generatesAndPersistsFirstManifest() {

        MovementManifest generated =
                service.generateAndPersist(movement.id());

        MovementManifest persisted =
                manifestRepository.findById(generated.id())
                        .orElseThrow();

        assertEquals(1, generated.version());
        assertEquals(generated, persisted);
    }

    @Test
    void allocatesSequentialManifestVersions() {

        MovementManifest first =
                service.generateAndPersist(movement.id());

        MovementManifest second =
                service.generateAndPersist(movement.id());

        MovementManifest third =
                service.generateAndPersist(movement.id());

        assertEquals(1, first.version());
        assertEquals(2, second.version());
        assertEquals(3, third.version());

        assertEquals(
                3,
                manifestRepository.findLatestVersion(
                        movement.id()
                )
        );
    }

    @Test
    void preservesPreviouslyGeneratedManifests() {

        MovementManifest first =
                service.generateAndPersist(movement.id());

        MovementManifest second =
                service.generateAndPersist(movement.id());

        assertEquals(
                first,
                manifestRepository.findByMovementIdAndVersion(
                        movement.id(),
                        1
                ).orElseThrow()
        );

        assertEquals(
                second,
                manifestRepository.findByMovementIdAndVersion(
                        movement.id(),
                        2
                ).orElseThrow()
        );

        assertNotEquals(first.id(), second.id());
    }

    @Test
    void retrievesLatestGeneratedManifest() {

        service.generateAndPersist(movement.id());

        MovementManifest latest =
                service.generateAndPersist(movement.id());

        assertEquals(
                latest,
                manifestRepository.findLatestByMovementId(
                        movement.id()
                ).orElseThrow()
        );
    }

    @Test
    void retrievesCompleteManifestHistory() {

        MovementManifest first =
                service.generateAndPersist(movement.id());

        MovementManifest second =
                service.generateAndPersist(movement.id());

        MovementManifest third =
                service.generateAndPersist(movement.id());

        assertEquals(
                List.of(third, second, first),
                manifestRepository.findByMovementId(
                        movement.id()
                )
        );
    }

    @Test
    void allocatesVersionsIndependentlyForEachMovement() {

        Movement otherMovement = createMovement();

        movementRepository.save(otherMovement);

        MovementManifest first =
                service.generateAndPersist(movement.id());

        MovementManifest second =
                service.generateAndPersist(movement.id());

        MovementManifest other =
                service.generateAndPersist(otherMovement.id());

        assertEquals(1, first.version());
        assertEquals(2, second.version());
        assertEquals(1, other.version());

        assertEquals(
                2,
                manifestRepository.findLatestVersion(
                        movement.id()
                )
        );

        assertEquals(
                1,
                manifestRepository.findLatestVersion(
                        otherMovement.id()
                )
        );
    }

    @Test
    void draftGenerationDoesNotConsumeVersion() {

        MovementManifest draft =
                service.generateDraft(movement.id());

        assertTrue(
                manifestRepository.findById(
                        draft.id()
                ).isEmpty()
        );

        assertEquals(
                0,
                manifestRepository.findLatestVersion(
                        movement.id()
                )
        );

        MovementManifest persisted =
                service.generateAndPersist(movement.id());

        assertEquals(1, persisted.version());
    }

    @Test
    void draftGenerationDoesNotAdvanceExistingVersion() {

        service.generateAndPersist(movement.id());

        service.generateDraft(movement.id());
        service.generateDraft(movement.id());

        assertEquals(
                1,
                manifestRepository.findLatestVersion(
                        movement.id()
                )
        );

        MovementManifest next =
                service.generateAndPersist(movement.id());

        assertEquals(2, next.version());
    }
}