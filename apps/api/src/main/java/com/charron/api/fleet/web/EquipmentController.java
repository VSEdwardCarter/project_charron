
package com.charron.api.fleet.web;

import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;
import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;
import com.charron.api.fleet.domain.EquipmentSpecifications;
import com.charron.api.fleet.domain.WeightSpecifications;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{id}/specifications")
    public ResponseEntity<EquipmentSpecificationsResponse> findSpecifications(
            @PathVariable UUID id
    ) {
        if (equipmentRepository.findIdentityById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return equipmentRepository
                .findSpecificationsByEquipmentId(id)
                .map(specifications -> ResponseEntity.ok(
                        toSpecificationsResponse(specifications)
                ))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private static EquipmentSpecificationsResponse toSpecificationsResponse(
            EquipmentSpecifications specifications
    ) {
        Dimensions dimensions = specifications.exteriorDimensions();
        WeightSpecifications weights = specifications.weightSpecifications();

        return new EquipmentSpecificationsResponse(
                dimensions == null ? null : dimensions.lengthMm(),
                dimensions == null ? null : dimensions.widthMm(),
                dimensions == null ? null : dimensions.heightMm(),
                weights != null,
                weights == null ? null : gramsOrNull(weights.emptyWeight()),
                weights == null ? null : gramsOrNull(weights.grossWeightRating()),
                weights == null ? null : gramsOrNull(weights.ratedPayloadCapacity())
        );
    }

    private static Long gramsOrNull(Weight weight) {
        return weight == null ? null : weight.grams();
    }

    public record EquipmentSpecificationsResponse(
            Long lengthMm,
            Long widthMm,
            Long heightMm,
            boolean weightSpecificationsPresent,
            Long emptyWeightGrams,
            Long grossWeightRatingGrams,
            Long ratedPayloadCapacityGrams
    ) {
    }

}

