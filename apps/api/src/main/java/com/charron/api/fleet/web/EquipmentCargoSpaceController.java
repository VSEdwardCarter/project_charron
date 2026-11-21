
package com.charron.api.fleet.web;

import com.charron.api.fleet.application.EquipmentCargoSpaceRepository;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;
import com.charron.api.fleet.domain.CargoSpaceType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/equipment/{equipmentId}/cargo-spaces")
public class EquipmentCargoSpaceController {

    private final EquipmentCargoSpaceRepository repository;

    public EquipmentCargoSpaceController(
            EquipmentCargoSpaceRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    @GetMapping
    public List<CargoSpaceResponse> findByEquipment(
            @PathVariable UUID equipmentId
    ) {
        return repository.findByEquipmentId(equipmentId)
                .stream()
                .map(CargoSpaceResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CargoSpaceResponse create(
            @PathVariable UUID equipmentId,
            @RequestBody CreateCargoSpaceRequest request
    ) {
        Objects.requireNonNull(request, "Request is required");

        CargoSpaceSpecifications specifications =
                new CargoSpaceSpecifications(
                        Objects.requireNonNull(
                                request.type(),
                                "Cargo space type is required"
                        ),
                        request.usableLengthMm(),
                        request.usableWidthMm(),
                        request.usableHeightMm()
                );

        UUID cargoSpaceId = UUID.randomUUID();

        repository.insert(
                cargoSpaceId,
                equipmentId,
                request.name(),
                specifications
        );

        return new CargoSpaceResponse(
                cargoSpaceId,
                equipmentId,
                request.name().trim(),
                specifications.type(),
                specifications.usableLengthMm(),
                specifications.usableWidthMm(),
                specifications.usableHeightMm()
        );
    }

    public record CreateCargoSpaceRequest(
            String name,
            CargoSpaceType type,
            long usableLengthMm,
            long usableWidthMm,
            Long usableHeightMm
    ) {}

    public record CargoSpaceResponse(
            UUID id,
            UUID equipmentId,
            String name,
            CargoSpaceType type,
            long usableLengthMm,
            long usableWidthMm,
            Long usableHeightMm
    ) {
        static CargoSpaceResponse from(
                EquipmentCargoSpaceRepository.EquipmentCargoSpace space
        ) {
            return new CargoSpaceResponse(
                    space.id(),
                    space.equipmentId(),
                    space.name(),
                    space.specifications().type(),
                    space.specifications().usableLengthMm(),
                    space.specifications().usableWidthMm(),
                    space.specifications().usableHeightMm()
            );
        }
    }
}
