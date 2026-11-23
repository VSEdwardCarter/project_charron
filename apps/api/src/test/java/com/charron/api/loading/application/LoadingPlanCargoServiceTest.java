
package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanCargo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoadingPlanCargoServiceTest {

    private LoadingPlanCargoRepository cargoRepository;
    private LoadingPlanService loadingPlanService;
    private LoadingPlanCargoService service;

    private UUID planId;
    private UUID cargoId;

    @BeforeEach
    void setUp() {
        cargoRepository = mock(LoadingPlanCargoRepository.class);
        loadingPlanService = mock(LoadingPlanService.class);

        service = new LoadingPlanCargoService(
                cargoRepository,
                loadingPlanService
        );

        planId = UUID.randomUUID();
        cargoId = UUID.randomUUID();

        LoadingPlan plan = LoadingPlan.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(loadingPlanService.getById(planId))
                .thenReturn(plan);
    }

    @Test
    void addsCargoToExistingPlan() {
        LoadingPlanCargo result =
                service.addCargo(planId, cargoId, 3);

        assertEquals(planId, result.loadingPlanId());
        assertEquals(cargoId, result.cargoId());
        assertEquals(3, result.quantity());

        verify(cargoRepository).insert(result);
    }

    @Test
    void rejectsDuplicateCargo() {
        when(cargoRepository.existsByLoadingPlanAndCargo(
                planId, cargoId
        )).thenReturn(true);

        assertThrows(
                LoadingPlanConflictException.class,
                () -> service.addCargo(planId, cargoId, 3)
        );

        verify(cargoRepository, never())
                .insert(any());
    }

    @Test
    void rejectsZeroQuantity() {
        assertThrows(
                InvalidLoadingPlanException.class,
                () -> service.addCargo(planId, cargoId, 0)
        );

        verify(cargoRepository, never())
                .insert(any());
    }

    @Test
    void rejectsUnknownLoadingPlan() {
        when(loadingPlanService.getById(planId))
                .thenThrow(new LoadingPlanNotFoundException(
                        "Loading plan does not exist"
                ));

        assertThrows(
                LoadingPlanNotFoundException.class,
                () -> service.addCargo(planId, cargoId, 3)
        );

        verify(cargoRepository, never())
                .insert(any());
    }

    @Test
    void listsCargoForPlan() {
        LoadingPlanCargo line =
                LoadingPlanCargo.create(planId, cargoId, 2);

        when(cargoRepository.findByLoadingPlanId(planId))
                .thenReturn(List.of(line));

        assertEquals(
                List.of(line),
                service.getCargo(planId)
        );
    }

    @Test
    void updatesQuantity() {
        LoadingPlanCargo existing =
                LoadingPlanCargo.create(planId, cargoId, 2);

        when(cargoRepository.findById(existing.id()))
                .thenReturn(Optional.of(existing));

        LoadingPlanCargo updated =
                service.updateQuantity(planId, existing.id(), 5);

        assertEquals(5, updated.quantity());
        assertEquals(existing.id(), updated.id());

        verify(cargoRepository).updateQuantity(updated);
    }

    @Test
    void rejectsUpdatingCargoFromAnotherPlan() {
        LoadingPlanCargo otherPlanCargo =
                LoadingPlanCargo.create(
                        UUID.randomUUID(),
                        cargoId,
                        2
                );

        when(cargoRepository.findById(otherPlanCargo.id()))
                .thenReturn(Optional.of(otherPlanCargo));

        assertThrows(
                LoadingPlanNotFoundException.class,
                () -> service.updateQuantity(
                        planId,
                        otherPlanCargo.id(),
                        5
                )
        );

        verify(cargoRepository, never())
                .updateQuantity(any());
    }

    @Test
    void removesCargoFromPlan() {
        LoadingPlanCargo line =
                LoadingPlanCargo.create(planId, cargoId, 2);

        when(cargoRepository.findById(line.id()))
                .thenReturn(Optional.of(line));

        service.removeCargo(planId, line.id());

        verify(cargoRepository).deleteById(line.id());
    }

    @Test
    void rejectsRemovingCargoFromAnotherPlan() {
        LoadingPlanCargo otherPlanCargo =
                LoadingPlanCargo.create(
                        UUID.randomUUID(),
                        cargoId,
                        2
                );

        when(cargoRepository.findById(otherPlanCargo.id()))
                .thenReturn(Optional.of(otherPlanCargo));

        assertThrows(
                LoadingPlanNotFoundException.class,
                () -> service.removeCargo(
                        planId,
                        otherPlanCargo.id()
                )
        );

        verify(cargoRepository, never())
                .deleteById(any());
    }
}
