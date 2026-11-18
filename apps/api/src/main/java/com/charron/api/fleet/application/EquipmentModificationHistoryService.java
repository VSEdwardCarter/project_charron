
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.EquipmentModificationLifecycle;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class EquipmentModificationHistoryService {

    private final EquipmentModificationRepository repository;

    public EquipmentModificationHistoryService(
            EquipmentModificationRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    public boolean isEffectiveAt(
            UUID modificationId,
            Instant effectiveAt
    ) {
        Objects.requireNonNull(
                modificationId, "Modification ID is required"
        );
        Objects.requireNonNull(
                effectiveAt, "Effective timestamp is required"
        );

        EquipmentModificationLifecycle lifecycle =
                EquipmentModificationLifecycle.reconstruct(
                        modificationId,
                        repository.findEventsByModificationId(
                                modificationId
                        )
                );

        return lifecycle.isEffectiveAt(effectiveAt);
    }

    public boolean isEffectiveAsKnownAt(
            UUID modificationId,
            Instant effectiveAt,
            Instant knownAt
    ) {
        Objects.requireNonNull(
                modificationId, "Modification ID is required"
        );
        Objects.requireNonNull(
                effectiveAt, "Effective timestamp is required"
        );
        Objects.requireNonNull(
                knownAt, "Known-at timestamp is required"
        );

        EquipmentModificationLifecycle lifecycle =
                EquipmentModificationLifecycle.reconstructAsKnownAt(
                        modificationId,
                        repository.findEventsByModificationId(
                                modificationId
                        ),
                        knownAt
                );

        return lifecycle.isEffectiveAt(effectiveAt);
    }
}
