
package com.charron.api.movement.application;

import com.charron.api.movement.domain.Movement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MovementServiceTest {

    private MovementRepository repository;
    private MovementService service;

    private static final Instant DEPARTURE =
            Instant.parse("2026-12-01T08:00:00Z");

    private static final Instant ARRIVAL =
            Instant.parse("2026-12-02T08:00:00Z");

    @BeforeEach
    void setUp() {
        repository = mock(MovementRepository.class);
        service = new MovementService(repository);
    }

    private Movement createMovement() {
        return Movement.plan(
                "MOV-001",
                "Denver, CO",
                "Dallas, TX",
                DEPARTURE,
                ARRIVAL
        );
    }

    @Test
    void createsAndPersistsPlannedMovement() {
        when(repository.existsByReference("MOV-001"))
                .thenReturn(false);

        Movement movement = service.create(
                "MOV-001",
                "Denver, CO",
                "Dallas, TX",
                DEPARTURE,
                ARRIVAL
        );

        assertEquals("MOV-001", movement.reference());
        assertEquals(
                com.charron.api.movement.domain.MovementStatus.PLANNED,
                movement.status()
        );

        verify(repository).save(movement);
    }

    @Test
    void rejectsDuplicateReference() {
        when(repository.existsByReference("MOV-001"))
                .thenReturn(true);

        assertThrows(
                DuplicateMovementReferenceException.class,
                () -> service.create(
                        "MOV-001",
                        "Denver, CO",
                        "Dallas, TX",
                        DEPARTURE,
                        ARRIVAL
                )
        );

        verify(repository, never()).save(any());
    }

    @Test
    void retrievesMovementById() {
        Movement movement = createMovement();

        when(repository.findById(movement.id()))
                .thenReturn(Optional.of(movement));

        assertEquals(
                movement,
                service.findById(movement.id()).orElseThrow()
        );
    }

    @Test
    void listsMovements() {
        Movement movement = createMovement();

        when(repository.findAll())
                .thenReturn(List.of(movement));

        assertEquals(
                List.of(movement),
                service.findAll()
        );
    }

    @Test
    void normalizesReferenceBeforeCheckingDuplicates() {
        when(repository.existsByReference("MOV-001"))
                .thenReturn(true);

        assertThrows(
                DuplicateMovementReferenceException.class,
                () -> service.create(
                        " MOV-001 ",
                        "Denver, CO",
                        "Dallas, TX",
                        DEPARTURE,
                        ARRIVAL
                )
        );

        verify(repository).existsByReference("MOV-001");
        verify(repository, never()).save(any());
    }
}
