
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.Equipment;

import java.util.Optional;
import java.util.UUID;

public interface EquipmentRepository {

    void insertIdentity(Equipment equipment);

    Optional<Equipment> findIdentityById(UUID id);
}
