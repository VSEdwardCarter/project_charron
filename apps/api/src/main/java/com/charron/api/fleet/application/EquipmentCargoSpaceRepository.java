
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.CargoSpaceSpecifications;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EquipmentCargoSpaceRepository {

    void insert(
            UUID id,
            UUID equipmentId,
            String name,
            CargoSpaceSpecifications specifications
    );

    List<EquipmentCargoSpace> findByEquipmentId(
            UUID equipmentId
    );

    record EquipmentCargoSpace(
            UUID id,
            UUID equipmentId,
            String name,
            CargoSpaceSpecifications specifications
    ) {}


    Optional<EquipmentCargoSpace> findById(
            UUID cargoSpaceId
    );

}
