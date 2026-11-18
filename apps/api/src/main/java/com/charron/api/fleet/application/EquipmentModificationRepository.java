
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.EquipmentModification;
import com.charron.api.fleet.domain.EquipmentModificationEvent;

import java.util.UUID;
import java.util.Optional;
import java.util.List;

public interface EquipmentModificationRepository {

    void insert(EquipmentModification modification);

    Optional<EquipmentModification> findById(UUID modificationId);

    void insertEvent(EquipmentModificationEvent event);

    List<EquipmentModificationEvent> findEventsByModificationId(
            UUID modificationId
    );

}
