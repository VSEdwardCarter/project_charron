
package com.charron.api.movement.web;

import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementStatus;

import java.time.Instant;
import java.util.UUID;

public record MovementResponse(
        UUID id,
        String reference,
        String origin,
        String destination,
        Instant plannedDeparture,
        Instant plannedArrival,
        MovementStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static MovementResponse from(Movement movement) {
        return new MovementResponse(
                movement.id(),
                movement.reference(),
                movement.origin(),
                movement.destination(),
                movement.plannedDeparture(),
                movement.plannedArrival(),
                movement.status(),
                movement.createdAt(),
                movement.updatedAt()
        );
    }
}
