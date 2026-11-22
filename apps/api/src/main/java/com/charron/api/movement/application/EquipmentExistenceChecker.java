
package com.charron.api.movement.application;

import java.util.UUID;

public interface EquipmentExistenceChecker {

    boolean existsById(UUID equipmentId);
}
