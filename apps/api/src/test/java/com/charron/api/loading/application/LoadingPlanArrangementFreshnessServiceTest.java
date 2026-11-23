
package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import com.charron.api.loading.domain.LoadingPlanCargo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoadingPlanArrangementFreshnessServiceTest {

    private LoadingPlanService loadingPlanService;
    private LoadingPlanCargoRepository cargoRepository;
    private LoadingCargoCatalogLookup catalogLookup;
    private LoadingPlanArrangementFreshnessService service;

    private UUID planId;
    private UUID cargoSpaceId;
    private UUID cargoId;

    private LoadingPlanArrangement arrangement;

    @BeforeEach
    void setUp() {
        loadingPlanService = mock(LoadingPlanService.class);
        cargoRepository = mock(LoadingPlanCargoRepository.class);
        catalogLookup = mock(LoadingCargoCatalogLookup.class);

        service = new LoadingPlanArrangementFreshnessService(
                loadingPlanService,
                cargoRepository,
                catalogLookup,
                new JsonMapper()
        );

        planId = UUID.randomUUID();
        cargoSpaceId = UUID.randomUUID();
        cargoId = UUID.randomUUID();

        LoadingPlan plan = LoadingPlan.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                cargoSpaceId
        );

        when(loadingPlanService.getById(planId))
                .thenReturn(plan);

        when(cargoRepository.findByLoadingPlanId(planId))
                .thenReturn(List.of(
                        LoadingPlanCargo.create(planId, cargoId, 3)
                ));

        when(catalogLookup.findById(cargoId))
                .thenReturn(Optional.of(
                        new LoadingCargoCatalogLookup.CargoDetails(
                                cargoId,
                                "Test Pallet",
                                1200L,
                                1000L,
                                1500L,
                                500000L
                        )
                ));

        arrangement = newArrangement(3);
    }

    private LoadingPlanArrangement newArrangement(int quantity) {
        String request = """
                {
                  "cargoSpaceId": "%s",
                  "cargo": [
                    {
                      "cargoId": "%s",
                      "name": "Test Pallet",
                      "quantity": %d,
                      "lengthMm": 1200,
                      "widthMm": 1000,
                      "heightMm": 1500,
                      "weightGrams": 500000
                    }
                  ]
                }
                """.formatted(cargoSpaceId, cargoId, quantity);

        String preview = """
                {
                  "cargoSpaceId": "%s",
                  "status": "VALID",
                  "reason": "Cargo placed",
                  "placements": []
                }
                """.formatted(cargoSpaceId);

        return LoadingPlanArrangement.create(
                planId,
                request,
                preview,
                "VALID"
        );
    }

    @Test
    void currentWhenCargoAndCatalogUnchanged() {
        assertTrue(service.isCurrent(arrangement));
    }

    @Test
    void staleWhenQuantityChanges() {
        when(cargoRepository.findByLoadingPlanId(planId))
                .thenReturn(List.of(
                        LoadingPlanCargo.create(planId, cargoId, 5)
                ));

        assertFalse(service.isCurrent(arrangement));
    }

    @Test
    void staleWhenCargoRemoved() {
        when(cargoRepository.findByLoadingPlanId(planId))
                .thenReturn(List.of());

        assertFalse(service.isCurrent(arrangement));
    }

    @Test
    void staleWhenCatalogDimensionsChange() {
        when(catalogLookup.findById(cargoId))
                .thenReturn(Optional.of(
                        new LoadingCargoCatalogLookup.CargoDetails(
                                cargoId,
                                "Test Pallet",
                                1400L,
                                1000L,
                                1500L,
                                500000L
                        )
                ));

        assertFalse(service.isCurrent(arrangement));
    }

    @Test
    void staleWhenCatalogWeightChanges() {
        when(catalogLookup.findById(cargoId))
                .thenReturn(Optional.of(
                        new LoadingCargoCatalogLookup.CargoDetails(
                                cargoId,
                                "Test Pallet",
                                1200L,
                                1000L,
                                1500L,
                                600000L
                        )
                ));

        assertFalse(service.isCurrent(arrangement));
    }

    @Test
    void staleWhenCatalogCargoMissing() {
        when(catalogLookup.findById(cargoId))
                .thenReturn(Optional.empty());

        assertFalse(service.isCurrent(arrangement));
    }

    @Test
    void staleWhenRequestContainsWrongCargoSpace() {
        String incorrectRequest = arrangement.requestSnapshot()
                .replace(
                        cargoSpaceId.toString(),
                        UUID.randomUUID().toString()
                );

        LoadingPlanArrangement incorrect =
                LoadingPlanArrangement.create(
                        planId,
                        incorrectRequest,
                        arrangement.previewSnapshot(),
                        "VALID"
                );

        assertFalse(service.isCurrent(incorrect));
    }

    @Test
    void staleWhenSavedRequestIsMalformed() {
        LoadingPlanArrangement malformed =
                LoadingPlanArrangement.create(
                        planId,
                        "{ invalid json }",
                        arrangement.previewSnapshot(),
                        "VALID"
                );

        assertFalse(service.isCurrent(malformed));
    }
}
