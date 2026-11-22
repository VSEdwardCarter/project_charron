
package com.charron.api.movement.application;

import com.charron.api.movement.domain.Movement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class MovementService {

    private final MovementRepository repository;

    public MovementService(MovementRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Transactional
    public Movement create(
            String reference,
            String origin,
            String destination,
            Instant plannedDeparture,
            Instant plannedArrival
    ) {
        Movement movement = Movement.plan(
                reference,
                origin,
                destination,
                plannedDeparture,
                plannedArrival
        );

        if (repository.existsByReference(movement.reference())) {
            throw new DuplicateMovementReferenceException(
                    movement.reference()
            );
        }

        repository.save(movement);

        return movement;
    }

    @Transactional(readOnly = true)
    public Optional<Movement> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Movement> findAll() {
        return repository.findAll();
    }
}
