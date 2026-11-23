
package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import com.charron.api.loading.domain.LoadingPlanCargo;
import com.charron.api.loading.web.LoadingPreviewRequest;
import com.charron.api.loading.web.LoadingPreviewResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoadingPlanArrangementGenerationServiceTest {

    private LoadingPlanService loadingPlanService;
    private LoadingPlanCargoRepository cargoRepository;
    private LoadingCargoCatalogLookup catalogLookup;
    private LoadingPreviewService previewService;
    private LoadingPlanArrangementService arrangementService;

    private LoadingPlanArrangementGenerationService service;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private UUID planId;
    private UUID cargoSpaceId;
    private UUID cargoId;

    @BeforeEach
    void setUp() {
        loadingPlanService = mock(LoadingPlanService.class);
        cargoRepository = mock(LoadingPlanCargoRepository.class);
        catalogLookup = mock(LoadingCargoCatalogLookup.class);
        previewService = mock(LoadingPreviewService.class);
        arrangementService = mock(LoadingPlanArrangementService.class);

        service = new LoadingPlanArrangementGenerationService(
                loadingPlanService,
                cargoRepository,
                catalogLookup,
                previewService,
                arrangementService,
                objectMapper
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
                        LoadingPlanCargo.create(
                                planId,
                                cargoId,
                                3
                        )
                ));

        when(catalogLookup.findById(cargoId))
                .thenReturn(java.util.Optional.of(
                        new LoadingCargoCatalogLookup.CargoDetails(
                                cargoId,
                                "Test Pallet",
                                1200L,
                                1000L,
                                1500L,
                                500000L
                        )
                ));

        when(previewService.preview(any()))
                .thenAnswer(invocation -> {
                    LoadingPreviewRequest request =
                            invocation.getArgument(0);

                    return new LoadingPreviewResponse(
                            request.cargoSpaceId(),
                            "VALID",
                            "Cargo placed successfully",
                            List.of()
                    );
                });

        when(arrangementService.save(
                eq(planId),
                anyString(),
                anyString()
        )).thenAnswer(invocation -> {
            String requestJson = invocation.getArgument(1);
            String previewJson = invocation.getArgument(2);

            return LoadingPlanArrangement.create(
                    planId,
                    requestJson,
                    previewJson,
                    "VALID"
            );
        });
    }

    @Test
    void generatesAndSavesArrangement() throws Exception {
        LoadingPlanArrangement result =
                service.generateAndSave(planId);

        assertEquals(planId, result.loadingPlanId());

        verify(previewService).preview(any());

        verify(arrangementService).save(
                eq(planId),
                anyString(),
                anyString()
        );
    }

    @Test
    void usesSavedCargoQuantityAndCatalogMeasurements() {
        service.generateAndSave(planId);

        ArgumentCaptor<LoadingPreviewRequest> captor =
                ArgumentCaptor.forClass(
                        LoadingPreviewRequest.class
                );

        verify(previewService).preview(captor.capture());

        LoadingPreviewRequest request =
                captor.getValue();

        assertEquals(cargoSpaceId, request.cargoSpaceId());
        assertEquals(1, request.cargo().size());

        LoadingPreviewRequest.CargoItem item =
                request.cargo().getFirst();

        assertEquals(cargoId, item.cargoId());
        assertEquals("Test Pallet", item.name());
        assertEquals(3L, item.quantity());
        assertEquals(1200L, item.lengthMm());
        assertEquals(1000L, item.widthMm());
        assertEquals(1500L, item.heightMm());
        assertEquals(500000L, item.weightGrams());
    }

    @Test
    void savesActualEngineResponse() throws Exception {
        service.generateAndSave(planId);

        ArgumentCaptor<String> requestCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> previewCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(arrangementService).save(
                eq(planId),
                requestCaptor.capture(),
                previewCaptor.capture()
        );

        JsonNode request =
                objectMapper.readTree(
                        requestCaptor.getValue()
                );

        JsonNode preview =
                objectMapper.readTree(
                        previewCaptor.getValue()
                );

        assertEquals(
                cargoSpaceId.toString(),
                request.path("cargoSpaceId").asText()
        );

        assertEquals(
                cargoId.toString(),
                request.path("cargo")
                        .get(0)
                        .path("cargoId")
                        .asText()
        );

        assertEquals(
                3,
                request.path("cargo")
                        .get(0)
                        .path("quantity")
                        .asInt()
        );

        assertEquals(
                "VALID",
                preview.path("status").asText()
        );

        assertEquals(
                "Cargo placed successfully",
                preview.path("reason").asText()
        );

        assertTrue(
                preview.path("placements").isArray()
        );
    }

    @Test
    void rejectsLoadingPlanWithoutCargo() {
        when(cargoRepository.findByLoadingPlanId(planId))
                .thenReturn(List.of());

        assertThrows(
                InvalidLoadingPlanException.class,
                () -> service.generateAndSave(planId)
        );

        verifyNoInteractions(previewService);
        verifyNoInteractions(arrangementService);
    }

    @Test
    void rejectsMissingCatalogCargo() {
        when(catalogLookup.findById(cargoId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                LoadingPlanNotFoundException.class,
                () -> service.generateAndSave(planId)
        );

        verifyNoInteractions(previewService);
        verifyNoInteractions(arrangementService);
    }

    @Test
    void rejectsIncompleteCargoDimensions() {
        when(catalogLookup.findById(cargoId))
                .thenReturn(java.util.Optional.of(
                        new LoadingCargoCatalogLookup.CargoDetails(
                                cargoId,
                                "Incomplete Pallet",
                                1200L,
                                null,
                                1500L,
                                500000L
                        )
                ));

        assertThrows(
                InvalidLoadingPlanException.class,
                () -> service.generateAndSave(planId)
        );

        verifyNoInteractions(previewService);
        verifyNoInteractions(arrangementService);
    }

    @Test
    void rejectsUnknownLoadingPlan() {
        when(loadingPlanService.getById(planId))
                .thenThrow(new LoadingPlanNotFoundException(
                        "Loading plan does not exist"
                ));

        assertThrows(
                LoadingPlanNotFoundException.class,
                () -> service.generateAndSave(planId)
        );

        verifyNoInteractions(previewService);
        verifyNoInteractions(arrangementService);
    }

    @Test
    void preservesInvalidEngineResult() throws Exception {

        doAnswer(invocation -> {
            LoadingPreviewRequest request =
                    invocation.getArgument(0);

            return new LoadingPreviewResponse(
                    request.cargoSpaceId(),
                    "INVALID",
                    "Cargo placement failed validation",
                    List.of()
            );
        }).when(previewService).preview(any());


        service.generateAndSave(planId);

        ArgumentCaptor<String> previewCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(arrangementService).save(
                eq(planId),
                anyString(),
                previewCaptor.capture()
        );

        JsonNode preview =
                objectMapper.readTree(
                        previewCaptor.getValue()
                );

        assertEquals(
                "INVALID",
                preview.path("status").asText()
        );

        assertEquals(
                "Cargo placement failed validation",
                preview.path("reason").asText()
        );
    }
}
