
package com.charron.api.loading.api;

import com.charron.api.loading.application.InvalidLoadingPlanException;
import com.charron.api.loading.application.LoadingPlanConflictException;
import com.charron.api.loading.application.LoadingPlanNotFoundException;
import com.charron.api.loading.application.LoadingPlanService;
import com.charron.api.loading.domain.LoadingPlan;
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

class LoadingPlanControllerTest {

    private LoadingPlanService service;
    private MockMvc mockMvc;

    private UUID movementId;
    private UUID equipmentId;
    private UUID cargoSpaceId;

    @BeforeEach
    void setUp() {
        service = mock(LoadingPlanService.class);


        mockMvc = MockMvcBuilders
                .standaloneSetup(new LoadingPlanController(service))
                .setControllerAdvice(new LoadingPlanExceptionHandler())
                .build();


        movementId = UUID.randomUUID();
        equipmentId = UUID.randomUUID();
        cargoSpaceId = UUID.randomUUID();
    }

    @Test
    void createsLoadingPlan() throws Exception {
        LoadingPlan plan = LoadingPlan.create(
                movementId,
                equipmentId,
                cargoSpaceId
        );

        when(service.create(
                movementId,
                equipmentId,
                cargoSpaceId
        )).thenReturn(plan);

        String requestBody = """
                {
                    "equipmentId": "%s",
                    "cargoSpaceId": "%s"
                }
                """.formatted(equipmentId, cargoSpaceId);

        mockMvc.perform(
                        post("/api/movements/{movementId}/loading-plans",
                                movementId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(plan.id().toString()))
                .andExpect(jsonPath("$.movementId")
                        .value(movementId.toString()))
                .andExpect(jsonPath("$.equipmentId")
                        .value(equipmentId.toString()))
                .andExpect(jsonPath("$.cargoSpaceId")
                        .value(cargoSpaceId.toString()));

        verify(service).create(
                movementId,
                equipmentId,
                cargoSpaceId
        );
    }

    @Test
    void retrievesLoadingPlanById() throws Exception {
        LoadingPlan plan = LoadingPlan.create(
                movementId,
                equipmentId,
                cargoSpaceId
        );

        when(service.getById(plan.id()))
                .thenReturn(plan);

        mockMvc.perform(
                        get("/api/loading-plans/{planId}", plan.id())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(plan.id().toString()))
                .andExpect(jsonPath("$.movementId")
                        .value(movementId.toString()));

        verify(service).getById(plan.id());
    }

    @Test
    void listsLoadingPlansForMovement() throws Exception {
        LoadingPlan plan = LoadingPlan.create(
                movementId,
                equipmentId,
                cargoSpaceId
        );

        when(service.getByMovementId(movementId))
                .thenReturn(List.of(plan));

        mockMvc.perform(
                        get("/api/movements/{movementId}/loading-plans",
                                movementId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(plan.id().toString()))
                .andExpect(jsonPath("$[0].equipmentId")
                        .value(equipmentId.toString()));

        verify(service).getByMovementId(movementId);
    }

    @Test
    void returns404ForUnknownPlan() throws Exception {
        UUID planId = UUID.randomUUID();

        when(service.getById(planId))
                .thenThrow(new LoadingPlanNotFoundException(
                        "Loading plan does not exist"
                ));

        mockMvc.perform(
                        get("/api/loading-plans/{planId}", planId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Loading plan does not exist"));
    }

    @Test
    void returns404ForUnknownMovement() throws Exception {
        when(service.getByMovementId(movementId))
                .thenThrow(new LoadingPlanNotFoundException(
                        "Movement does not exist"
                ));

        mockMvc.perform(
                        get("/api/movements/{movementId}/loading-plans",
                                movementId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Movement does not exist"));
    }

    @Test
    void returns409ForDuplicatePlan() throws Exception {
        when(service.create(movementId, equipmentId, cargoSpaceId))
                .thenThrow(new LoadingPlanConflictException(
                        "A loading plan already exists"
                ));

        mockMvc.perform(
                        post("/api/movements/{movementId}/loading-plans",
                                movementId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestBody())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("A loading plan already exists"));
    }

    @Test
    void returns409ForUnassignedEquipment() throws Exception {
        when(service.create(movementId, equipmentId, cargoSpaceId))
                .thenThrow(new LoadingPlanConflictException(
                        "Equipment is not assigned to the movement"
                ));

        mockMvc.perform(
                        post("/api/movements/{movementId}/loading-plans",
                                movementId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestBody())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("Equipment is not assigned to the movement"));
    }

    @Test
    void returns400ForWrongCargoSpace() throws Exception {
        when(service.create(movementId, equipmentId, cargoSpaceId))
                .thenThrow(new InvalidLoadingPlanException(
                        "Cargo space does not belong to the selected equipment"
                ));

        mockMvc.perform(
                        post("/api/movements/{movementId}/loading-plans",
                                movementId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestBody())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Cargo space does not belong to the selected equipment"));
    }

    private String createRequestBody() {
        return """
            {
                "equipmentId": "%s",
                "cargoSpaceId": "%s"
            }
            """.formatted(equipmentId, cargoSpaceId);
    }

}
