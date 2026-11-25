
package com.charron.api.movement.web;

import com.charron.api.movement.application.MovementService;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/movements")
public class MovementController {

    private final MovementService service;

    public MovementController(MovementService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MovementResponse> create(
            @RequestBody MovementCreationRequest request
    ) {
        Movement movement = service.create(
                request.reference(),
                request.origin(),
                request.destination(),
                request.plannedDeparture(),
                request.plannedArrival()
        );

        return ResponseEntity
                .created(URI.create("/api/movements/" + movement.id()))
                .body(MovementResponse.from(movement));
    }

    @GetMapping
    public List<MovementResponse> findAll() {
        return service.findAll().stream()
                .map(MovementResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovementResponse> findById(
            @PathVariable UUID id
    ) {
        return service.findById(id)
                .map(MovementResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/dispatch")
    public MovementResponse dispatch(@PathVariable UUID id) {
        return transition(id, MovementStatus.DISPATCHED);
    }

    @PostMapping("/{id}/start")
    public MovementResponse start(@PathVariable UUID id) {
        return transition(id, MovementStatus.IN_TRANSIT);
    }

    @PostMapping("/{id}/complete")
    public MovementResponse complete(@PathVariable UUID id) {
        return transition(id, MovementStatus.COMPLETED);
    }

    @PostMapping("/{id}/cancel")
    public MovementResponse cancel(@PathVariable UUID id) {
        return transition(id, MovementStatus.CANCELLED);
    }

    private MovementResponse transition(
            UUID id,
            MovementStatus targetStatus
    ) {
        Movement movement = service.transition(id, targetStatus);
        return MovementResponse.from(movement);
    }
}
