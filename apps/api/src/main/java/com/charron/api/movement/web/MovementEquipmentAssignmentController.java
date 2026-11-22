
package com.charron.api.movement.web;

import com.charron.api.movement.application.MovementEquipmentAssignmentService;
import com.charron.api.movement.domain.MovementEquipmentAssignment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/movements/{movementId}/equipment")
public class MovementEquipmentAssignmentController {

    private final MovementEquipmentAssignmentService service;

    public MovementEquipmentAssignmentController(
            MovementEquipmentAssignmentService service
    ) {
        this.service = Objects.requireNonNull(service);
    }

    @PostMapping
    public ResponseEntity<MovementEquipmentAssignmentResponse> assign(
            @PathVariable UUID movementId,
            @RequestBody MovementEquipmentAssignmentRequest request
    ) {
        if (request.equipmentId() == null || request.role() == null) {
            throw new IllegalArgumentException(
                    "Equipment ID and assignment role are required"
            );
        }

        MovementEquipmentAssignment assignment = service.assign(
                movementId,
                request.equipmentId(),
                request.role()
        );

        URI location = URI.create(
                "/api/movements/" + movementId
                        + "/equipment/" + assignment.id()
        );

        return ResponseEntity
                .created(location)
                .body(MovementEquipmentAssignmentResponse.from(assignment));
    }

    @GetMapping
    public ResponseEntity<List<MovementEquipmentAssignmentResponse>> findAll(
            @PathVariable UUID movementId
    ) {
        List<MovementEquipmentAssignmentResponse> assignments =
                service.findByMovementId(movementId)
                        .stream()
                        .map(MovementEquipmentAssignmentResponse::from)
                        .toList();

        return ResponseEntity.ok(assignments);
    }
}
