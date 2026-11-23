
package com.charron.api.loading.application;

import java.util.UUID;

public interface CargoSpaceOwnershipChecker {

    boolean belongsToEquipment(
            UUID cargoSpaceId,
            UUID equipmentId
    );
}
