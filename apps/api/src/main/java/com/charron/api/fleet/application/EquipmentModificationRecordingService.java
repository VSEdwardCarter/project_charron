
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.EquipmentModification;
import com.charron.api.fleet.domain.EquipmentModificationEvent;
import com.charron.api.fleet.domain.EquipmentModificationEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class EquipmentModificationRecordingService {

    private final EquipmentModificationRepository repository;

    public EquipmentModificationRecordingService(
            EquipmentModificationRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Transactional
    public EquipmentModificationEvent record(
            EquipmentModification modification
    ) {
        Objects.requireNonNull(
                modification, "Equipment modification is required"
        );

        if (modification.removedAt() != null) {
            throw new IllegalArgumentException(
                    "A new modification cannot already be removed"
            );
        }

        EquipmentModificationEvent event =
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        modification.id(),
                        EquipmentModificationEventType.RECORDED,
                        modification.effectiveAt(),
                        modification.recordedAt()
                );

        repository.insert(modification);
        repository.insertEvent(event);

        return event;
    }
}
