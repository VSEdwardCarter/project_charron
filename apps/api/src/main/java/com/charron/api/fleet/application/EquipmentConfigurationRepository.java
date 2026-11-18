
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.EquipmentCapability;
import com.charron.api.fleet.domain.EquipmentConfiguration;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface EquipmentConfigurationRepository {

    void insert(EquipmentConfiguration configuration);

    Optional<EquipmentConfiguration> findById(UUID configurationId);

    void insertCapability(
            UUID configurationId,
            EquipmentCapability capability
    );

    Set<EquipmentCapability> findCapabilitiesByConfigurationId(
            UUID configurationId
    );

    boolean removeCapability(
            UUID configurationId,
            EquipmentCapability capability
    );

    boolean updateDetails(EquipmentConfiguration configuration);

}
