
package com.charron.api.movement.application;

import java.util.UUID;

public class EquipmentNotFoundException extends RuntimeException {

    public EquipmentNotFoundException(UUID equipmentId) {
        super("Equipment does not exist: " + equipmentId);
    }
}
