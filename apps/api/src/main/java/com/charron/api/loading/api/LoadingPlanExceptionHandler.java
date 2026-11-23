
package com.charron.api.loading.api;

import com.charron.api.loading.application.InvalidLoadingPlanException;
import com.charron.api.loading.application.LoadingPlanConflictException;
import com.charron.api.loading.application.LoadingPlanNotFoundException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(
        assignableTypes = {
                LoadingPlanController.class,
                LoadingPlanCargoController.class,
                LoadingPlanArrangementController.class,
                LoadingPlanCurrentArrangementController.class
        }
)
public class LoadingPlanExceptionHandler {

    @ExceptionHandler(LoadingPlanNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            LoadingPlanNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(InvalidLoadingPlanException.class)
    public ResponseEntity<Map<String, String>> handleInvalid(
            InvalidLoadingPlanException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(LoadingPlanConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(
            LoadingPlanConflictException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error",
                        "Loading plan operation conflicts "
                                + "with existing database records"
                ));
    }
}
