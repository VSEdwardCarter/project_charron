
package com.charron.api.movement.domain;

import java.util.EnumSet;
import java.util.Set;

public enum MovementStatus {

    PLANNED,
    DISPATCHED,
    IN_TRANSIT,
    COMPLETED,
    CANCELLED;

    public Set<MovementStatus> allowedTransitions() {
        return switch (this) {
            case PLANNED -> EnumSet.of(
                    DISPATCHED,
                    CANCELLED
            );

            case DISPATCHED -> EnumSet.of(
                    IN_TRANSIT,
                    CANCELLED
            );

            case IN_TRANSIT -> EnumSet.of(
                    COMPLETED
            );

            case COMPLETED, CANCELLED ->
                    EnumSet.noneOf(MovementStatus.class);
        };
    }

    public boolean canTransitionTo(MovementStatus target) {
        return target != null &&
                allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
