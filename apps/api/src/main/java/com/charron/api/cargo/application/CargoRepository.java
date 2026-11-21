
package com.charron.api.cargo.application;

import com.charron.api.cargo.domain.Cargo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CargoRepository {

    void insert(Cargo cargo);

    Optional<Cargo> findById(UUID cargoId);

    List<Cargo> findAll();
}
