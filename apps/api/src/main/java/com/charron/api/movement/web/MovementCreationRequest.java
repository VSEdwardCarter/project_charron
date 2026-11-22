
package com.charron.api.movement.web;

import java.time.Instant;

public record MovementCreationRequest(
        String reference,
        String origin,
        String destination,
        Instant plannedDeparture,
        Instant plannedArrival
) {
}
