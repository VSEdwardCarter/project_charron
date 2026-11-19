
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EquipmentRegistrationService {

    private final EquipmentRepository equipmentRepository;
    private final VehicleVinDecoder vinDecoder;
    private final VinDecodeSnapshotRepository snapshotRepository;

    public EquipmentRegistrationService(
            EquipmentRepository equipmentRepository,
            VehicleVinDecoder vinDecoder,
            VinDecodeSnapshotRepository snapshotRepository
    ) {
        this.equipmentRepository = equipmentRepository;
        this.vinDecoder = vinDecoder;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public Equipment register(
            String name,
            EquipmentCategory category,
            String vin
    ) {
        Equipment equipment = new Equipment(
                UUID.randomUUID(),
                name,
                category,
                vin
        );

        // Decode before inserting the equipment identity.
        VinDecodeSnapshot snapshot = null;

        if (equipment.getVin() != null) {
            snapshot = vinDecoder.decodeSnapshot(
                    equipment.getVin()
            );
        }

        equipmentRepository.insertIdentity(equipment);

        if (snapshot != null) {
            snapshotRepository.save(
                    equipment.getId(),
                    snapshot
            );
        }

        return equipment;
    }
}
