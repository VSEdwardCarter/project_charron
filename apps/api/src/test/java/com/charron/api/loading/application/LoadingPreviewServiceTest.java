
package com.charron.api.loading.application;

import com.charron.api.fleet.application.EquipmentCargoSpaceRepository;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;
import com.charron.api.fleet.domain.CargoSpaceType;
import com.charron.api.loading.web.LoadingPreviewRequest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoadingPreviewServiceTest {

    private final EquipmentCargoSpaceRepository repository =
            mock(EquipmentCargoSpaceRepository.class);

    private final LoadingPreviewService service =
            new LoadingPreviewService(repository);

    private final UUID equipmentId = UUID.randomUUID();
    private final UUID cargoSpaceId = UUID.randomUUID();

    private void registerTestCargoSpace() {
        var specifications = new CargoSpaceSpecifications(
                CargoSpaceType.ENCLOSED,
                7000,
                2400,
                2500L
        );

        var stored = new EquipmentCargoSpaceRepository.EquipmentCargoSpace(
                cargoSpaceId,
                equipmentId,
                "Main Cargo Compartment",
                specifications
        );

        when(repository.findById(cargoSpaceId))
                .thenReturn(Optional.of(stored));
    }

    @Test
    void generatesValidMixedCargoPreview() {
        registerTestCargoSpace();

        var request = new LoadingPreviewRequest(
                cargoSpaceId,
                List.of(
                        new LoadingPreviewRequest.CargoItem(
                                UUID.randomUUID(),
                                "Generator",
                                3,
                                1200,
                                800,
                                1000,
                                150000L
                        ),
                        new LoadingPreviewRequest.CargoItem(
                                UUID.randomUUID(),
                                "Supply Crate",
                                4,
                                600,
                                600,
                                600,
                                25000L
                        )
                )
        );

        var response = service.preview(request);

        assertEquals("VALID", response.status(), response.reason());
        assertEquals(cargoSpaceId, response.cargoSpaceId());
        assertEquals(7, response.placements().size());

        for (var placement : response.placements()) {
            assertEquals(cargoSpaceId, placement.cargoSpaceId());
            assertEquals(0, placement.zMm());
            assertNotNull(placement.loadCargoLineId());
        }

        verify(repository).findById(cargoSpaceId);
    }

    @Test
    void rejectsUnknownCargoSpace() {
        UUID unknownSpaceId = UUID.randomUUID();

        var request = new LoadingPreviewRequest(
                unknownSpaceId,
                List.of(
                        new LoadingPreviewRequest.CargoItem(
                                UUID.randomUUID(),
                                "Generator",
                                1,
                                1200,
                                800,
                                1000,
                                150000L
                        )
                )
        );

        var response = service.preview(request);

        assertEquals("INVALID", response.status());
        assertTrue(response.reason().contains("not found"));
        assertTrue(response.placements().isEmpty());
    }

    @Test
    void reportsIndeterminateWhenDimensionsAreMissing() {
        registerTestCargoSpace();

        var request = new LoadingPreviewRequest(
                cargoSpaceId,
                List.of(
                        new LoadingPreviewRequest.CargoItem(
                                UUID.randomUUID(),
                                "Unmeasured Freight",
                                1,
                                0,
                                800,
                                1000,
                                null
                        )
                )
        );

        var response = service.preview(request);

        assertEquals("INDETERMINATE", response.status());
        assertTrue(response.placements().isEmpty());
    }

    @Test
    void reportsWhenPlacementStrategyCannotFitCargo() {
        registerTestCargoSpace();

        var request = new LoadingPreviewRequest(
                cargoSpaceId,
                List.of(
                        new LoadingPreviewRequest.CargoItem(
                                UUID.randomUUID(),
                                "Oversized Equipment",
                                1,
                                9000,
                                3000,
                                1000,
                                500000L
                        )
                )
        );

        var response = service.preview(request);

        assertEquals("NOT_PLACED", response.status());
        assertTrue(response.placements().isEmpty());
        assertTrue(
                response.reason().contains("Unable to place")
        );
    }

    @Test
    void rejectsEmptyCargoRequest() {
        var request = new LoadingPreviewRequest(
                cargoSpaceId,
                List.of()
        );

        var response = service.preview(request);

        assertEquals("INVALID", response.status());
        assertTrue(
                response.reason().contains(
                        "At least one cargo item"
                )
        );
        assertTrue(response.placements().isEmpty());

        verifyNoInteractions(repository);
    }

}
