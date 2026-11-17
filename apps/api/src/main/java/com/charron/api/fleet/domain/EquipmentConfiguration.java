
package com.charron.api.fleet.domain;

import java.util.Objects;
import java.util.UUID;

public class EquipmentConfiguration {

    private final UUID id;
    private final UUID equipmentId;
    private String name;
    private boolean active;

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
}
