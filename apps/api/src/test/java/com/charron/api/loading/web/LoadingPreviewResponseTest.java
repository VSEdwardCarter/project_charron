
package com.charron.api.loading.web;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoadingPreviewContractTest {

    @Test
    void representsLoadingPreviewRequest() {
        UUID spaceId = UUID.randomUUID();
        UUID cargoId = UUID.randomUUID();

        var item = new LoadingPreviewRequest.CargoItem(
                cargoId,
                "Generator",
                3,
                1200,
                800,
                1000,
                150000L
        );

        var request = new LoadingPreviewRequest(
                spaceId,
                List.of(item)
        );

        assertEquals(spaceId, request.cargoSpaceId());
        assertEquals(1, request.cargo().size());
        assertEquals(3, request.cargo().getFirst().quantity());
    }

    @Test
    void representsRenderReadyPlacementResponse() {
        UUID spaceId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID cargoId = UUID.randomUUID();

        var placement =
                new LoadingPreviewResponse.CargoPlacementResponse(
                        lineId,
                        cargoId,
                        spaceId,
                        0,
                        1200,
                        800,
                        0,
                        "ORIGINAL",
                        1200,
                        800,
                        1000,
                        "Generator"
                );

        var response = new LoadingPreviewResponse(
                spaceId,
                "VALID",
                "Loading plan passed validation",
                List.of(placement)
        );

        assertEquals("VALID", response.status());
        assertEquals(1, response.placements().size());
        assertEquals(1200, response.placements()
                .getFirst().xMm());
        assertEquals(lineId, response.placements()
                .getFirst().loadCargoLineId());
    }
}
