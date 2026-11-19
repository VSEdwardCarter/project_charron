
package com.charron.api.fleet.web;

import com.charron.api.fleet.application.EquipmentModificationRecordingService;
import com.charron.api.fleet.application.EquipmentModificationRepository;
import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.domain.EquipmentModification;
import com.charron.api.fleet.domain.EquipmentModificationEvent;
import com.charron.api.fleet.domain.EquipmentModificationType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/equipment/{equipmentId}/modifications")
public class EquipmentModificationController {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentModificationRecordingService recordingService;
    private final EquipmentModificationRepository modificationRepository;


    public EquipmentModificationController(
            EquipmentRepository equipmentRepository,
            EquipmentModificationRecordingService recordingService,
            EquipmentModificationRepository modificationRepository
    ) {
        this.equipmentRepository = equipmentRepository;
        this.recordingService = recordingService;
        this.modificationRepository = modificationRepository;
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModificationResponse record(
            @PathVariable UUID equipmentId,
            @RequestBody RecordModificationRequest request
    ) {
        if (equipmentRepository.findIdentityById(equipmentId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Equipment not found"
            );
        }

        if (request == null
                || request.type() == null
                || request.description() == null
                || request.description().isBlank()
                || request.effectiveAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Type, description, and effectiveAt are required"
            );
        }

        Instant recordedAt = Instant.now();

        EquipmentModification modification = new EquipmentModification(
                UUID.randomUUID(),
                equipmentId,
                null,
                request.type(),
                request.description(),
                request.effectiveAt(),
                recordedAt
        );

        EquipmentModificationEvent event =
                recordingService.record(modification);

        return new ModificationResponse(
                modification.id(),
                modification.equipmentId(),
                modification.type(),
                modification.description(),
                modification.effectiveAt(),
                modification.recordedAt(),
                event.id()
        );
    }

    public record RecordModificationRequest(
            EquipmentModificationType type,
            String description,
            Instant effectiveAt
    ) {
    }

    public record ModificationResponse(
            UUID id,
            UUID equipmentId,
            EquipmentModificationType type,
            String description,
            Instant effectiveAt,
            Instant recordedAt,
            UUID eventId
    ) {
    }

    @GetMapping
    public List<EquipmentModification> findAll(
            @PathVariable UUID equipmentId
    ) {
        if (equipmentRepository.findIdentityById(equipmentId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Equipment not found"
            );
        }

        return modificationRepository.findByEquipmentId(equipmentId);
    }

}
