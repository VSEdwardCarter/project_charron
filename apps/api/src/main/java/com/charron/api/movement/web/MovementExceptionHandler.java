
package com.charron.api.movement.web;

import com.charron.api.movement.application.DuplicateMovementReferenceException;
import com.charron.api.movement.application.MovementNotFoundException;
import com.charron.api.movement.application.MovementTransitionConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice(assignableTypes = {
        MovementController.class,
        DispatchAssessmentController.class
})

public class MovementExceptionHandler {

    @ExceptionHandler(DuplicateMovementReferenceException.class)
    public ProblemDetail handleDuplicate(
            DuplicateMovementReferenceException exception
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Duplicate Movement Reference",
                exception.getMessage()
        );
    }

    @ExceptionHandler(MovementNotFoundException.class)
    public ProblemDetail handleNotFound(
            MovementNotFoundException exception
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Movement Not Found",
                exception.getMessage()
        );
    }

    @ExceptionHandler(MovementTransitionConflictException.class)
    public ProblemDetail handleTransitionConflict(
            MovementTransitionConflictException exception
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Movement Transition Conflict",
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleInvalidTransition(
            IllegalStateException exception
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Invalid Movement Transition",
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalid(
            IllegalArgumentException exception
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid Movement",
                exception.getMessage()
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                detail
        );

        problem.setTitle(title);
        return problem;
    }
}
