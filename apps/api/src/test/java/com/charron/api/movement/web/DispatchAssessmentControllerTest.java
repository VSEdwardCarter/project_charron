
package com.charron.api.movement.web;

import com.charron.api.movement.application.DispatchAssessment;
import com.charron.api.movement.application.DispatchAssessmentService;
import com.charron.api.movement.application.MovementNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DispatchAssessmentControllerTest {

    private DispatchAssessmentService service;
    private DispatchAssessmentController controller;
    private MovementExceptionHandler exceptionHandler;

    private UUID movementId;

    @BeforeEach
    void setUp() {
        service = mock(DispatchAssessmentService.class);
        controller = new DispatchAssessmentController(service);
        exceptionHandler = new MovementExceptionHandler();
        movementId = UUID.randomUUID();
    }


    @Test
    void returnsAssessmentWithoutFindings() {
        DispatchAssessment assessment = new DispatchAssessment(
                movementId,
                List.of()
        );

        when(service.assess(movementId))
                .thenReturn(assessment);

        DispatchAssessment response = controller.assess(movementId);

        assertEquals(movementId, response.movementId());
        assertTrue(response.findings().isEmpty());
        assertFalse(response.hasBlockingFindings());

        verify(service).assess(movementId);
    }

    @Test
    void returnsAssessmentWithBlockingFindings() {
        DispatchAssessment assessment = new DispatchAssessment(
                movementId,
                List.of(
                        new DispatchAssessment.Finding(
                                "LOADING_ARRANGEMENT_MISSING",
                                DispatchAssessment.Severity.BLOCKER,
                                "Loading arrangement has not been generated"
                        )
                )
        );

        when(service.assess(movementId))
                .thenReturn(assessment);

        DispatchAssessment response =
                controller.assess(movementId);

        assertEquals(movementId, response.movementId());
        assertTrue(response.hasBlockingFindings());
        assertEquals(1, response.findings().size());
        assertEquals(
                "LOADING_ARRANGEMENT_MISSING",
                response.findings().getFirst().code()
        );

        verify(service).assess(movementId);
    }

    @Test
    void propagatesMissingMovement() {
        when(service.assess(movementId))
                .thenThrow(
                        new MovementNotFoundException(movementId)
                );

        assertThrows(
                MovementNotFoundException.class,
                () -> controller.assess(movementId)
        );
    }

    @Test
    void mapsMissingMovementTo404() {
        ProblemDetail problem = exceptionHandler.handleNotFound(
                new MovementNotFoundException(movementId)
        );

        assertEquals(404, problem.getStatus());
        assertEquals("Movement Not Found", problem.getTitle());
    }
}
