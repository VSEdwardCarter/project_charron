
package com.charron.api.loading.api;

import com.charron.api.loading.application.InvalidLoadingPlanException;
import com.charron.api.loading.application.LoadingPlanCargoService;
import com.charron.api.loading.application.LoadingPlanConflictException;
import com.charron.api.loading.application.LoadingPlanNotFoundException;
import com.charron.api.loading.domain.LoadingPlanCargo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoadingPlanCargoControllerTest {

    private LoadingPlanCargoService service;
    private MockMvc mockMvc;

    private UUID planId;
    private UUID cargoId;

    @BeforeEach
    void setUp() {
        service = mock(LoadingPlanCargoService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new LoadingPlanCargoController(service))
                .setControllerAdvice(new LoadingPlanExceptionHandler())
                .build();

        planId = UUID.randomUUID();
        cargoId = UUID.randomUUID();
    }

    @Test
    void addsCargo() throws Exception {
        LoadingPlanCargo line =
                LoadingPlanCargo.create(planId, cargoId, 3);

        when(service.addCargo(planId, cargoId, 3))
                .thenReturn(line);

        mockMvc.perform(
                        post("/api/loading-plans/{planId}/cargo", planId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "cargoId": "%s",
                                  "quantity": 3
                                }
                                """.formatted(cargoId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(line.id().toString()))
                .andExpect(jsonPath("$.quantity").value(3));

        verify(service).addCargo(planId, cargoId, 3);
    }

    @Test
    void listsCargo() throws Exception {
        LoadingPlanCargo line =
                LoadingPlanCargo.create(planId, cargoId, 2);

        when(service.getCargo(planId))
                .thenReturn(List.of(line));

        mockMvc.perform(
                        get("/api/loading-plans/{planId}/cargo", planId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cargoId")
                        .value(cargoId.toString()))
                .andExpect(jsonPath("$[0].quantity").value(2));
    }

    @Test
    void updatesQuantity() throws Exception {
        UUID lineId = UUID.randomUUID();

        LoadingPlanCargo updated =
                LoadingPlanCargo.create(planId, cargoId, 5);

        when(service.updateQuantity(planId, lineId, 5))
                .thenReturn(updated);

        mockMvc.perform(
                        put(
                                "/api/loading-plans/{planId}/cargo/{cargoLineId}",
                                planId,
                                lineId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {"quantity": 5}
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));

        verify(service).updateQuantity(planId, lineId, 5);
    }

    @Test
    void removesCargo() throws Exception {
        UUID lineId = UUID.randomUUID();

        mockMvc.perform(
                        delete(
                                "/api/loading-plans/{planId}/cargo/{cargoLineId}",
                                planId,
                                lineId
                        )
                )
                .andExpect(status().isNoContent());

        verify(service).removeCargo(planId, lineId);
    }

    @Test
    void returns404ForUnknownPlan() throws Exception {
        when(service.getCargo(planId))
                .thenThrow(new LoadingPlanNotFoundException(
                        "Loading plan does not exist"
                ));

        mockMvc.perform(
                        get("/api/loading-plans/{planId}/cargo", planId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Loading plan does not exist"));
    }

    @Test
    void returns409ForDuplicateCargo() throws Exception {
        when(service.addCargo(planId, cargoId, 3))
                .thenThrow(new LoadingPlanConflictException(
                        "Cargo is already assigned"
                ));

        mockMvc.perform(
                        post("/api/loading-plans/{planId}/cargo", planId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "cargoId": "%s",
                                  "quantity": 3
                                }
                                """.formatted(cargoId))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void returns400ForInvalidQuantity() throws Exception {
        when(service.addCargo(planId, cargoId, 0))
                .thenThrow(new InvalidLoadingPlanException(
                        "Cargo quantity must be greater than zero"
                ));

        mockMvc.perform(
                        post("/api/loading-plans/{planId}/cargo", planId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "cargoId": "%s",
                                  "quantity": 0
                                }
                                """.formatted(cargoId))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns400ForMissingCargoId() throws Exception {
        mockMvc.perform(
                        post("/api/loading-plans/{planId}/cargo", planId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {"quantity": 3}
                                """)
                )
                .andExpect(status().isBadRequest());
    }
}
