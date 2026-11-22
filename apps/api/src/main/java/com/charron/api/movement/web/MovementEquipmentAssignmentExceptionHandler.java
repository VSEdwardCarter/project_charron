
package com.charron.api.movement.web;

import com.charron.api.movement.application.DuplicateMovementEquipmentAssignmentException;
import com.charron.api.movement.application.EquipmentNotFoundException;
import com.charron.api.movement.application.MovementNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes = MovementEquipmentAssignmentController.class
)
public class MovementEquipmentAssignmentExceptionHandler {

    @ExceptionHandler(MovementNotFoundException.class)
    public ProblemDetail handleMovementNotFound(
            MovementNotFoundException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.NOT_FOUND
        );

        problem.setTitle("Movement Not Found");
        problem.setDetail(exception.getMessage());

        return problem;
    }

    @ExceptionHandler(
            DuplicateMovementEquipmentAssignmentException.class
    )
    public ProblemDetail handleDuplicateAssignment(
            DuplicateMovementEquipmentAssignmentException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.CONFLICT
        );

        problem.setTitle("Duplicate Equipment Assignment");
        problem.setDetail(exception.getMessage());

        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidAssignment(
            IllegalArgumentException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.BAD_REQUEST
        );

        problem.setTitle("Invalid Equipment Assignment");
        problem.setDetail(exception.getMessage());

        return problem;
    }

    @ExceptionHandler(EquipmentNotFoundException.class)
    public ProblemDetail handleEquipmentNotFound(
            EquipmentNotFoundException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.NOT_FOUND
        );

        problem.setTitle("Equipment Not Found");
        problem.setDetail(exception.getMessage());

        return problem;
    }

}
