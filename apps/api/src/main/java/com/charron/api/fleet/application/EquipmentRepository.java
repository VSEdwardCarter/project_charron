
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentSpecifications;
import com.charron.api.fleet.domain.EquipmentCapability;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface EquipmentRepository {


    List<Equipment> findAllIdentities();


    void insertCapability(
            UUID equipmentId,
            EquipmentCapability capability
    );

    void insertSpecifications(
            UUID equipmentId,
            EquipmentSpecifications specifications
    );

    void insertIdentity(Equipment equipment);

    Optional<Equipment> findIdentityById(UUID id);


    Optional<EquipmentSpecifications> findSpecificationsByEquipmentId(
            UUID equipmentId
    );


    Set<EquipmentCapability> findCapabilitiesByEquipmentId(
            UUID equipmentId
    );

    boolean removeCapability(
            UUID equipmentId,
            EquipmentCapability capability
    );


}
