
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.EquipmentModification;

import java.util.UUID;
import java.util.Optional;

public interface EquipmentModificationRepository {

    void insert(EquipmentModification modification);

    Optional<EquipmentModification> findById(UUID modificationId);

}
