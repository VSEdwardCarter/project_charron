
package com.charron.api.fleet.web;

import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
                .map(EquipmentController::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentResponse register(
            @RequestBody EquipmentRegistrationRequest request
    ) {
        Equipment equipment = new Equipment(
                UUID.randomUUID(),
                request.name(),
                request.category(),
                request.vin()
        );

        equipmentRepository.insertIdentity(equipment);

        return toResponse(equipment);
    }

    private static EquipmentResponse toResponse(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getVin()
        );
    }

    public record EquipmentRegistrationRequest(
            String name,
            EquipmentCategory category,
            String vin
    ) {
    }

    public record EquipmentResponse(
            UUID id,
            String name,
            EquipmentCategory category,
            String vin
    ) {
    }
}
