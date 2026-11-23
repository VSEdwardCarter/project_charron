
package com.charron.api.loading.api;

import com.charron.api.loading.application.LoadingPlanArrangementFreshnessService;
import com.charron.api.loading.application.LoadingPlanArrangementService;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoadingPlanCurrentArrangementControllerTest {

    private LoadingPlanArrangementService arrangementService;
    private LoadingPlanArrangementFreshnessService freshnessService;
    private MockMvc mockMvc;

    private UUID planId;
    private LoadingPlanArrangement arrangement;

    @BeforeEach
    void setUp() {
        arrangementService = mock(LoadingPlanArrangementService.class);
        freshnessService = mock(LoadingPlanArrangementFreshnessService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new LoadingPlanCurrentArrangementController(
                                arrangementService,
                                freshnessService
                        )
                )
                .setControllerAdvice(new LoadingPlanExceptionHandler())
                .build();

        planId = UUID.randomUUID();
        UUID cargoSpaceId = UUID.randomUUID();

        arrangement = LoadingPlanArrangement.create(
                planId,
                """
                {"cargoSpaceId":"%s","cargo":[]}
                """.formatted(cargoSpaceId),
                """
                {
                  "cargoSpaceId":"%s",
                  "status":"VALID",
                  "reason":"Cargo positioned",
                  "placements":[]
                }
                """.formatted(cargoSpaceId),
                "VALID"
        );
    }

    @Test
    void returnsCurrentArrangement() throws Exception {
        when(arrangementService.findByLoadingPlanId(planId))
                .thenReturn(Optional.of(arrangement));

        when(freshnessService.isCurrent(arrangement))
                .thenReturn(true);

        mockMvc.perform(
                        get("/api/loading-plans/{planId}/arrangement/current", planId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadingPlanId")
                        .value(planId.toString()))
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.previewSnapshot").isString());

        verify(freshnessService).isCurrent(arrangement);
    }

    @Test
    void returns409ForOutdatedArrangement() throws Exception {
        when(arrangementService.findByLoadingPlanId(planId))
                .thenReturn(Optional.of(arrangement));

        when(freshnessService.isCurrent(arrangement))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/loading-plans/{planId}/arrangement/current", planId)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value(org.hamcrest.Matchers.containsString("outdated")));
    }

    @Test
    void returns404WhenNoArrangementExists() throws Exception {
        when(arrangementService.findByLoadingPlanId(planId))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/loading-plans/{planId}/arrangement/current", planId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value(org.hamcrest.Matchers.containsString(
                                "No saved arrangement"
                        )));

        verifyNoInteractions(freshnessService);
    }

    @Test
    void returns400ForMalformedPlanId() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/loading-plans/{planId}/arrangement/current",
                                "invalid-uuid"
                        )
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(arrangementService, freshnessService);
    }
}
