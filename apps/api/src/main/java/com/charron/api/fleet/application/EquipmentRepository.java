
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentSpecifications;

import java.util.Optional;
import java.util.UUID;

public interface EquipmentRepository {


    void insertSpecifications(
            UUID equipmentId,
            EquipmentSpecifications specifications
    );

    void insertIdentity(Equipment equipment);

    Optional<Equipment> findIdentityById(UUID id);


    Optional<EquipmentSpecifications> findSpecificationsByEquipmentId(
            UUID equipmentId
    );

}
