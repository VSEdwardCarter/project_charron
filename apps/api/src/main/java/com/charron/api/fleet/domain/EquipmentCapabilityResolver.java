
package com.charron.api.fleet.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class EquipmentCapabilityResolver {

    private EquipmentCapabilityResolver() {
    }

    public static Set<EquipmentCapability> resolve(
            Equipment equipment,
            EquipmentConfiguration configuration
    ) {
        Objects.requireNonNull(
                equipment, "Equipment is required"
        );

        EnumSet<EquipmentCapability> effective =
                EnumSet.noneOf(EquipmentCapability.class);

        effective.addAll(equipment.getCapabilities());

        if (configuration != null) {
            if (!equipment.getId().equals(
                    configuration.getEquipmentId())) {
                throw new IllegalArgumentException(
                        "Configuration does not belong to equipment"
                );
            }

            if (!configuration.isActive()) {
                throw new IllegalStateException(
                        "Configuration must be active"
                );
            }

            effective.addAll(configuration.getCapabilities());
        }

        return Collections.unmodifiableSet(effective);
    }
}
