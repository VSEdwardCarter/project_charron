
package com.charron.api.movement.web;

import com.charron.api.movement.application.DuplicateMovementReferenceException;
import com.charron.api.movement.application.MovementService;
import com.charron.api.movement.domain.Movement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MovementControllerTest {

    private MovementService service;
    private MovementController controller;
    private MovementExceptionHandler exceptionHandler;

    private static final Instant DEPARTURE =
            Instant.parse("2026-12-01T08:00:00Z");

    private static final Instant ARRIVAL =
            Instant.parse("2026-12-02T08:00:00Z");

    @BeforeEach
    void setUp() {
        service = mock(MovementService.class);
        controller = new MovementController(service);
        exceptionHandler = new MovementExceptionHandler();
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
    void createsMovement() {
        Movement movement = createMovement();

        when(service.create(
                eq("MOV-001"),
                eq("Denver, CO"),
                eq("Dallas, TX"),
                eq(DEPARTURE),
                eq(ARRIVAL)
        )).thenReturn(movement);

        MovementCreationRequest request =
                new MovementCreationRequest(
                        "MOV-001",
                        "Denver, CO",
                        "Dallas, TX",
                        DEPARTURE,
                        ARRIVAL
                );

        ResponseEntity<MovementResponse> response =
                controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(
                "/api/movements/" + movement.id(),
                response.getHeaders().getLocation().toString()
        );
        assertNotNull(response.getBody());
        assertEquals(movement.id(), response.getBody().id());
        assertEquals("MOV-001", response.getBody().reference());
    }

    @Test
    void listsMovements() {
        Movement movement = createMovement();

        when(service.findAll()).thenReturn(List.of(movement));

        List<MovementResponse> response = controller.findAll();

        assertEquals(1, response.size());
        assertEquals(movement.id(), response.getFirst().id());
    }

    @Test
    void retrievesMovementById() {
        Movement movement = createMovement();

        when(service.findById(movement.id()))
                .thenReturn(Optional.of(movement));

        ResponseEntity<MovementResponse> response =
                controller.findById(movement.id());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(movement.id(), response.getBody().id());
    }

    @Test
    void returnsNotFoundForUnknownMovement() {
        UUID id = UUID.randomUUID();

        when(service.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<MovementResponse> response =
                controller.findById(id);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void mapsDuplicateReferenceToConflict() {
        ProblemDetail problem = exceptionHandler.handleDuplicate(
                new DuplicateMovementReferenceException("MOV-001")
        );

        assertEquals(409, problem.getStatus());
        assertEquals("Duplicate Movement Reference", problem.getTitle());
    }

    @Test
    void mapsInvalidMovementToBadRequest() {
        ProblemDetail problem = exceptionHandler.handleInvalid(
                new IllegalArgumentException("Invalid movement schedule")
        );

        assertEquals(400, problem.getStatus());
        assertEquals("Invalid Movement", problem.getTitle());
    }
}
