
package com.charron.api.cargo.web;

import com.charron.api.cargo.application.CargoRepository;
import com.charron.api.cargo.domain.Cargo;
import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/cargo")
public class CargoController {

    private final CargoRepository repository;

    public CargoController(CargoRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CargoResponse register(
            @RequestBody CargoRegistrationRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cargo request is required"
            );
        }

        try {
            Cargo cargo = new Cargo(
                    UUID.randomUUID(),
                    request.name(),
                    request.reference(),
                    request.description()
            );

            Long weightGrams = request.weightGrams();
            Long length = request.lengthMm();
            Long width = request.widthMm();
            Long height = request.heightMm();

            boolean anyDimension =
                    length != null || width != null || height != null;

            boolean allDimensions =
                    length != null && width != null && height != null;

            if (anyDimension && !allDimensions) {
                throw new IllegalArgumentException(
                        "All three dimensions must be provided together"
                );
            }

            Weight weight = weightGrams == null
                    ? null
                    : new Weight(weightGrams);

            if (weight != null && weight.grams() == 0) {
                throw new IllegalArgumentException(
                        "Cargo weight must be greater than zero"
                );
            }

            Dimensions dimensions = allDimensions
                    ? new Dimensions(length, width, height)
                    : null;

            if (weight != null || dimensions != null) {
                cargo.setMeasurements(
                        new CargoMeasurements(weight, dimensions)
                );
            }

            repository.insert(cargo);

            return CargoResponse.from(cargo);

        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
    }

    @GetMapping
    public List<CargoResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(CargoResponse::from)
                .toList();
    }

    @GetMapping("/{cargoId}")
    public CargoResponse findById(
            @PathVariable UUID cargoId
    ) {
        return repository.findById(cargoId)
                .map(CargoResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cargo not found"
                ));
    }
}
