
package com.charron.api.loading.web;

import java.util.List;
import java.util.UUID;

public record LoadingPreviewResponse(
        UUID cargoSpaceId,
        String status,
        String reason,
        List<CargoPlacementResponse> placements
) {

    public record CargoPlacementResponse(
            UUID loadCargoLineId,
            UUID cargoId,
            UUID cargoSpaceId,
            long unitIndex,
            long xMm,
            long yMm,
            long zMm,
            String orientation,
            long lengthMm,
            long widthMm,
            long heightMm,
            String label
    ) {
    }
}
