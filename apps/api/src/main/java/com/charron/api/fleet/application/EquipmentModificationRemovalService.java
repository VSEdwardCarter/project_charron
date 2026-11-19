
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.EquipmentModification;
import com.charron.api.fleet.domain.EquipmentModificationEvent;
import com.charron.api.fleet.domain.EquipmentModificationEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class EquipmentModificationRemovalService {

    private final EquipmentModificationRepository repository;

    public EquipmentModificationRemovalService(
            EquipmentModificationRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Transactional
    public EquipmentModificationEvent remove(
            UUID modificationId,
            Instant effectiveAt
    ) {
        Objects.requireNonNull(
                modificationId,
                "Modification ID is required"
        );
        Objects.requireNonNull(
                effectiveAt,
                "Removal effective date is required"
        );

        EquipmentModification modification = repository
                .findById(modificationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Modification not found"
                ));

        if (modification.removedAt() != null) {
            throw new IllegalStateException(
                    "Modification has already been removed"
            );
        }

        if (effectiveAt.isBefore(modification.effectiveAt())) {
            throw new IllegalArgumentException(
                    "Removal cannot precede modification effective date"
            );
        }

        boolean updated = repository.markRemoved(
                modificationId,
                effectiveAt
        );

        if (!updated) {
            throw new IllegalStateException(
                    "Modification could not be marked as removed"
            );
        }

        EquipmentModificationEvent event =
                new EquipmentModificationEvent(
                        UUID.randomUUID(),
                        modificationId,
                        EquipmentModificationEventType.REMOVED,
                        effectiveAt,
                        Instant.now()
                );

        repository.insertEvent(event);

        return event;
    }
}
