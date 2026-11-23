
package com.charron.api.loading.api;

import com.charron.api.loading.application.InvalidLoadingPlanException;
import com.charron.api.loading.application.LoadingPlanArrangementGenerationService;
import com.charron.api.loading.application.LoadingPlanArrangementService;
import com.charron.api.loading.application.LoadingPlanNotFoundException;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoadingPlanArrangementControllerTest {

    private LoadingPlanArrangementGenerationService generationService;
    private LoadingPlanArrangementService arrangementService;
    private MockMvc mockMvc;

    private UUID planId;
    private UUID cargoSpaceId;
    private LoadingPlanArrangement arrangement;

    @BeforeEach
    void setUp() {
        generationService =
                mock(LoadingPlanArrangementGenerationService.class);

        arrangementService =
                mock(LoadingPlanArrangementService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new LoadingPlanArrangementController(
                                generationService,
                                arrangementService
                        )
                )
                .setControllerAdvice(new LoadingPlanExceptionHandler())
                .build();

        planId = UUID.randomUUID();
        cargoSpaceId = UUID.randomUUID();

        String requestSnapshot = """
                {
                  "cargoSpaceId": "%s",
                  "cargo": []
                }
                """.formatted(cargoSpaceId);

        String previewSnapshot = """
                {
                  "cargoSpaceId": "%s",
                  "status": "VALID",
                  "reason": "Cargo positioned",
                  "placements": []
                }
                """.formatted(cargoSpaceId);

        arrangement = LoadingPlanArrangement.create(
                planId,
                requestSnapshot,
                previewSnapshot,
                "VALID"
        );
    }

    @Test
    void generatesAndSavesArrangement() throws Exception {
        when(generationService.generateAndSave(planId))
                .thenReturn(arrangement);

        mockMvc.perform(
                        post(
                                "/api/loading-plans/{planId}/arrangement/generate",
                                planId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadingPlanId")
                        .value(planId.toString()))
                .andExpect(jsonPath("$.formatVersion").value(1))
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.requestSnapshot").isString())
                .andExpect(jsonPath("$.previewSnapshot").isString());

        verify(generationService).generateAndSave(planId);
    }

    @Test
    void retrievesSavedArrangement() throws Exception {
        when(arrangementService.findByLoadingPlanId(planId))
                .thenReturn(Optional.of(arrangement));

        mockMvc.perform(
                        get(
                                "/api/loading-plans/{planId}/arrangement",
                                planId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadingPlanId")
                        .value(planId.toString()))
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.savedAt").exists());

        verify(arrangementService).findByLoadingPlanId(planId);
    }

    @Test
    void returns404WhenArrangementDoesNotExist() throws Exception {
        when(arrangementService.findByLoadingPlanId(planId))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get(
                                "/api/loading-plans/{planId}/arrangement",
                                planId
                        )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void returns404WhenLoadingPlanDoesNotExist() throws Exception {
        when(arrangementService.findByLoadingPlanId(planId))
                .thenThrow(new LoadingPlanNotFoundException(
                        "Loading plan does not exist"
                ));

        mockMvc.perform(
                        get(
                                "/api/loading-plans/{planId}/arrangement",
                                planId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Loading plan does not exist"));
    }

    @Test
    void rejectsGenerationWithoutAssignedCargo() throws Exception {
        when(generationService.generateAndSave(planId))
                .thenThrow(new InvalidLoadingPlanException(
                        "Add cargo to the loading plan before generating an arrangement"
                ));

        mockMvc.perform(
                        post(
                                "/api/loading-plans/{planId}/arrangement/generate",
                                planId
                        )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value(
                                "Add cargo to the loading plan before generating an arrangement"
                        ));
    }

    @Test
    void returns404WhenGeneratingForUnknownPlan() throws Exception {
        when(generationService.generateAndSave(planId))
                .thenThrow(new LoadingPlanNotFoundException(
                        "Loading plan does not exist"
                ));

        mockMvc.perform(
                        post(
                                "/api/loading-plans/{planId}/arrangement/generate",
                                planId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Loading plan does not exist"));
    }

    @Test
    void rejectsInvalidPlanId() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/loading-plans/{planId}/arrangement",
                                "not-a-uuid"
                        )
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(arrangementService);
    }

    @Test
    void preservesInvalidEngineResult() throws Exception {
        LoadingPlanArrangement invalidArrangement =
                LoadingPlanArrangement.create(
                        planId,
                        arrangement.requestSnapshot(),
                        """
                        {
                          "status": "INVALID",
                          "reason": "Cargo placement failed",
                          "placements": []
                        }
                        """,
                        "INVALID"
                );

        when(generationService.generateAndSave(planId))
                .thenReturn(invalidArrangement);

        mockMvc.perform(
                        post(
                                "/api/loading-plans/{planId}/arrangement/generate",
                                planId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID"));

        verify(generationService).generateAndSave(planId);
    }
}
