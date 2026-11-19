
package com.charron.api.fleet.web;

import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;
import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Set;
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


    @PutMapping("/{id}/specifications")
    public EquipmentSpecificationsResponse saveSpecifications(
            @PathVariable UUID id,
            @RequestBody EquipmentSpecificationsRequest request
    ) {
        if (equipmentRepository.findIdentityById(id).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Equipment not found"
            );
        }

        try {
            boolean hasDimensions =
                    request.lengthMm() != null
                            || request.widthMm() != null
                            || request.heightMm() != null;

            Dimensions dimensions = hasDimensions
                    ? new Dimensions(
                    requireValue(request.lengthMm(), "Length"),
                    requireValue(request.widthMm(), "Width"),
                    requireValue(request.heightMm(), "Height")
            )
                    : null;

            boolean hasWeightValues =
                    request.emptyWeightGrams() != null
                            || request.grossWeightRatingGrams() != null
                            || request.ratedPayloadCapacityGrams() != null;

            if (!request.weightSpecificationsPresent()
                    && hasWeightValues) {
                throw new IllegalArgumentException(
                        "Weight values require weight specifications"
                );
            }

            WeightSpecifications weights =
                    request.weightSpecificationsPresent()
                            ? new WeightSpecifications(
                            toWeight(request.emptyWeightGrams()),
                            toWeight(request.grossWeightRatingGrams()),
                            toWeight(request.ratedPayloadCapacityGrams())
                    )
                            : null;

            EquipmentSpecifications specifications =
                    new EquipmentSpecifications(dimensions, weights);

            equipmentRepository.saveSpecifications(id, specifications);

            return toSpecificationsResponse(specifications);

        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
    }

    private static long requireValue(Long value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(
                    name + " is required when dimensions are provided"
            );
        }

        return value;
    }

    private static Weight toWeight(Long grams) {
        return grams == null ? null : new Weight(grams);
    }

    public record EquipmentSpecificationsRequest(
            Long lengthMm,
            Long widthMm,
            Long heightMm,
            boolean weightSpecificationsPresent,
            Long emptyWeightGrams,
            Long grossWeightRatingGrams,
            Long ratedPayloadCapacityGrams
    ) {
    }


    @GetMapping("/{id}/capabilities")
    public ResponseEntity<Set<EquipmentCapability>> findCapabilities(
            @PathVariable UUID id
    ) {
        if (equipmentRepository.findIdentityById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                equipmentRepository.findCapabilitiesByEquipmentId(id)
        );
    }


    @PostMapping("/{id}/capabilities")
    @ResponseStatus(HttpStatus.CREATED)
    public void addCapability(
            @PathVariable UUID id,
            @RequestBody EquipmentCapabilityRequest request
    ) {
        if (equipmentRepository.findIdentityById(id).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Equipment not found"
            );
        }

        if (request == null || request.capability() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Equipment capability is required"
            );
        }

        try {
            equipmentRepository.insertCapability(
                    id,
                    request.capability()
            );
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Equipment already has this capability",
                    exception
            );
        }
    }

    public record EquipmentCapabilityRequest(
            EquipmentCapability capability
    ) {
    }

    @DeleteMapping("/{id}/capabilities/{capability}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCapability(
            @PathVariable UUID id,
            @PathVariable EquipmentCapability capability
    ) {
        if (equipmentRepository.findIdentityById(id).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Equipment not found"
            );
        }

        boolean removed = equipmentRepository.removeCapability(
                id,
                capability
        );

        if (!removed) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Equipment capability not assigned"
            );
        }
    }


}

