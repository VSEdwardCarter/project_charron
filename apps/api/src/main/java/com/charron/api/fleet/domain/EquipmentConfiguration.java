
package com.charron.api.fleet.domain;

import java.util.Objects;
import java.util.UUID;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class EquipmentConfiguration {

    private final UUID id;
    private final UUID equipmentId;
    private String name;
    private boolean active;
    private final Set<EquipmentCapability> capabilities =
            EnumSet.noneOf(EquipmentCapability.class);
    private ConfigurationWeightAdjustment weightAdjustment;

    public EquipmentConfiguration(
            UUID id,
            UUID equipmentId,
            String name,
            boolean active
    ) {
        this.id = Objects.requireNonNull(
                id, "Configuration ID is required"
        );

        this.equipmentId = Objects.requireNonNull(
                equipmentId, "Equipment ID is required"
        );

        setName(name);
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEquipmentId() {
        return equipmentId;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Configuration name is required"
            );
        }

        this.name = name.trim();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }


    public Set<EquipmentCapability> getCapabilities() {
        return Collections.unmodifiableSet(capabilities);
    }

    public void addCapability(EquipmentCapability capability) {
        capabilities.add(
                Objects.requireNonNull(
                        capability,
                        "Configuration capability is required"
                )
        );
    }

    public void removeCapability(EquipmentCapability capability) {
        capabilities.remove(
                Objects.requireNonNull(
                        capability,
                        "Configuration capability is required"
                )
        );
    }

    public boolean hasCapability(EquipmentCapability capability) {
        return capabilities.contains(
                Objects.requireNonNull(
                        capability,
                        "Configuration capability is required"
                )
        );
    }


    public ConfigurationWeightAdjustment getWeightAdjustment() {
        return weightAdjustment;
    }

    public void updateWeightAdjustment(
            ConfigurationWeightAdjustment adjustment
    ) {
        this.weightAdjustment = Objects.requireNonNull(
                adjustment,
                "Configuration weight adjustment is required"
        );
    }

    public void removeWeightAdjustment() {
        this.weightAdjustment = null;
    }


}
