
package com.charron.api.fleet.web;

import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.application.VinDecodeSnapshot;
import com.charron.api.fleet.application.VinDecodeSnapshotRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/equipment/{equipmentId}/vin-decodes")
public class EquipmentVinDecodeController {

    private final EquipmentRepository equipmentRepository;
    private final VinDecodeSnapshotRepository snapshotRepository;

    public EquipmentVinDecodeController(
            EquipmentRepository equipmentRepository,
            VinDecodeSnapshotRepository snapshotRepository
    ) {
        this.equipmentRepository = equipmentRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @GetMapping("/latest")
    public ResponseEntity<VinDecodeSnapshot> findLatest(
            @PathVariable UUID equipmentId
    ) {
        if (equipmentRepository.findIdentityById(equipmentId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return snapshotRepository
                .findLatestByEquipmentId(equipmentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
