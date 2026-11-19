
package com.charron.api.fleet.application;

import java.util.Optional;
import java.util.UUID;

public interface VinDecodeSnapshotRepository {

    void save(
            UUID equipmentId,
            VinDecodeSnapshot snapshot
    );

    Optional<VinDecodeSnapshot> findLatestByEquipmentId(
            UUID equipmentId
    );
}
