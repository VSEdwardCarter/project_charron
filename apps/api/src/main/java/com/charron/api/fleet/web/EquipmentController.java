
package com.charron.api.fleet.web;

import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentRepository equipmentRepository;

    public EquipmentController(
            EquipmentRepository equipmentRepository
    ) {
        this.equipmentRepository = equipmentRepository;
    }

    @GetMapping
    public List<EquipmentResponse> findAll() {
        return equipmentRepository.findAllIdentities()
                .stream()
                .map(equipment -> new EquipmentResponse(
                        equipment.getId(),
                        equipment.getName(),
                        equipment.getCategory(),
                        equipment.getVin()
                ))
                .toList();
    }

    public record EquipmentResponse(
            UUID id,
            String name,
            EquipmentCategory category,
            String vin
    ) {
    }
}
