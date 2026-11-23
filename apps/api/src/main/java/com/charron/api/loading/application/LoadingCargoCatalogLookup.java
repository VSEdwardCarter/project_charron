
package com.charron.api.loading.application;

import java.util.Optional;
import java.util.UUID;

public interface LoadingCargoCatalogLookup {

    record CargoDetails(
            UUID id,
            String name,
            Long lengthMm,
            Long widthMm,
            Long heightMm,
            Long weightGrams
    ) {}

    Optional<CargoDetails> findById(UUID cargoId);
}
