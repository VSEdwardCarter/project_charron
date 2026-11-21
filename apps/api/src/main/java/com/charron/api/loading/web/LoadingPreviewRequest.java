
package com.charron.api.loading.web;

import java.util.List;
import java.util.UUID;

public record LoadingPreviewRequest(
        UUID cargoSpaceId,
        List<CargoItem> cargo
) {

    public record CargoItem(
            UUID cargoId,
            String name,
            long quantity,
            long lengthMm,
            long widthMm,
            long heightMm,
            Long weightGrams
    ) {
    }
}
