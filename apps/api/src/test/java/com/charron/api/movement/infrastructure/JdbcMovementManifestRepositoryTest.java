package com.charron.api.movement.infrastructure;

import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;
import com.charron.api.movement.domain.MovementEquipmentRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JdbcMovementManifestRepositoryTest {

    @Autowired
    private JdbcMovementManifestRepository repository;

    @Autowired
    private JdbcMovementRepository movementRepository;

    private Movement movement;

    @Autowired
    private JdbcTemplate jdbcTemplate;



    @BeforeEach
    void setUp() {

        movement = Movement.plan(
                "TEST-MANIFEST-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);
    }

    private MovementManifest createManifest(
            int version,
            int quantity
    ) {

        UUID equipmentId = UUID.randomUUID();
        UUID loadingPlanId = UUID.randomUUID();
        UUID cargoSpaceId = UUID.randomUUID();
        UUID cargoId = UUID.randomUUID();

        return new MovementManifest(
                UUID.randomUUID(),
                movement.id(),
                version,
                movement.reference(),
                movement.origin(),
                movement.destination(),
                movement.plannedDeparture(),
                movement.plannedArrival(),

                List.of(
                        new MovementManifest.EquipmentEntry(
                                equipmentId,
                                "Test Trailer",
                                MovementEquipmentRole.CARGO_CARRIER,
                                null
                        )
                ),

                List.of(
                        new MovementManifest.CargoEntry(
                                loadingPlanId,
                                cargoId,
                                "Test Pallet",
                                quantity,
                                1200L,
                                1000L,
                                1500L,
                                250000L
                        )
                ),

                List.of(
                        new MovementManifest.LoadingEntry(
                                loadingPlanId,
                                equipmentId,
                                cargoSpaceId,
                                "VALID",
                                """
                                {
                                    "placements": [],
                                    "valid": true
                                }
                                """
                        )
                ),

                Instant.parse("2026-11-24T12:00:00Z")
                        .plusSeconds(version)
        );
    }

    @Test
    void insertsAndRetrievesCompleteManifest() {

        MovementManifest manifest = createManifest(1, 10);

        repository.save(manifest);

        MovementManifest restored =
                repository.findById(manifest.id())
                        .orElseThrow();

        assertEquals(manifest, restored);

        assertEquals(
                2500000L,
                restored.cargo().getFirst().totalWeightGrams()
        );
    }

    @Test
    void retrievesManifestByMovementAndVersion() {

        MovementManifest first = createManifest(1, 10);
        MovementManifest second = createManifest(2, 20);

        repository.save(first);
        repository.save(second);

        assertEquals(
                first,
                repository.findByMovementIdAndVersion(
                        movement.id(),
                        1
                ).orElseThrow()
        );

        assertEquals(
                second,
                repository.findByMovementIdAndVersion(
                        movement.id(),
                        2
                ).orElseThrow()
        );
    }

    @Test
    void retrievesLatestManifest() {

        MovementManifest first = createManifest(1, 10);
        MovementManifest second = createManifest(2, 20);
        MovementManifest third = createManifest(3, 30);

        repository.save(first);
        repository.save(second);
        repository.save(third);

        assertEquals(
                third,
                repository.findLatestByMovementId(
                        movement.id()
                ).orElseThrow()
        );
    }

    @Test
    void retrievesHistoryInDescendingVersionOrder() {

        MovementManifest first = createManifest(1, 10);
        MovementManifest second = createManifest(2, 20);
        MovementManifest third = createManifest(3, 30);

        repository.save(first);
        repository.save(second);
        repository.save(third);

        List<MovementManifest> history =
                repository.findByMovementId(movement.id());

        assertEquals(
                List.of(third, second, first),
                history
        );
    }

    @Test
    void returnsZeroWhenNoManifestExists() {

        assertEquals(
                0,
                repository.findLatestVersion(movement.id())
        );
    }

    @Test
    void returnsHighestExistingVersion() {

        repository.save(createManifest(1, 10));
        repository.save(createManifest(2, 20));
        repository.save(createManifest(3, 30));

        assertEquals(
                3,
                repository.findLatestVersion(movement.id())
        );
    }

    @Test
    void returnsEmptyForUnknownManifest() {

        assertTrue(
                repository.findById(
                        UUID.randomUUID()
                ).isEmpty()
        );
    }

    @Test
    void returnsEmptyWhenMovementHasNoManifestHistory() {

        assertTrue(
                repository.findByMovementId(
                        movement.id()
                ).isEmpty()
        );

        assertTrue(
                repository.findLatestByMovementId(
                        movement.id()
                ).isEmpty()
        );

        assertTrue(
                repository.findByMovementIdAndVersion(
                        movement.id(),
                        1
                ).isEmpty()
        );
    }

    @Test
    void rejectsDuplicateMovementManifestVersion() {

        MovementManifest first = createManifest(1, 10);
        MovementManifest duplicate = createManifest(1, 20);

        repository.save(first);

        assertThrows(
                DuplicateKeyException.class,
                () -> repository.save(duplicate)
        );
    }

    @Test
    void rejectsDuplicateManifestIdentity() {

        MovementManifest manifest = createManifest(1, 10);

        repository.save(manifest);

        assertThrows(
                DuplicateKeyException.class,
                () -> repository.save(manifest)
        );
    }

    @Test
    void preservesHistoricalManifestSnapshots() {

        MovementManifest original = createManifest(1, 10);
        MovementManifest revised = createManifest(2, 25);

        repository.save(original);
        repository.save(revised);

        MovementManifest historical =
                repository.findByMovementIdAndVersion(
                        movement.id(),
                        1
                ).orElseThrow();

        MovementManifest latest =
                repository.findLatestByMovementId(
                        movement.id()
                ).orElseThrow();

        assertEquals(original, historical);
        assertEquals(revised, latest);

        assertEquals(
                10,
                historical.cargo().getFirst().quantity()
        );

        assertEquals(
                25,
                latest.cargo().getFirst().quantity()
        );
    }
    @Test
    void rejectsDirectManifestUpdate() {

        MovementManifest manifest = createManifest(1, 10);

        repository.save(manifest);

        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update("""
                    UPDATE movement_manifests
                    SET version = ?
                    WHERE id = ?
                    """,
                        2,
                        manifest.id()
                )
        );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains("Movement manifests are immutable")
        );
    }

    @Test
    void rejectsDirectManifestDeletion() {

        MovementManifest manifest = createManifest(1, 10);

        repository.save(manifest);

        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update("""
                    DELETE FROM movement_manifests
                    WHERE id = ?
                    """,
                        manifest.id()
                )
        );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains("Movement manifests are immutable")
        );
    }






}