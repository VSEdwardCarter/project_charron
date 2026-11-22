
package com.charron.api.movement.application;

import java.util.UUID;

public class MovementNotFoundException
        extends RuntimeException {

    public MovementNotFoundException(UUID movementId) {
        super("Movement does not exist: " + movementId);
    }
}
