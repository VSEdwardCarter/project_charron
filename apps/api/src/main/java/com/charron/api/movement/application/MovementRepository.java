
package com.charron.api.movement.application;

import com.charron.api.movement.domain.Movement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovementRepository {

    void save(Movement movement);

    Optional<Movement> findById(UUID id);

    List<Movement> findAll();

    boolean existsByReference(String reference);
}
