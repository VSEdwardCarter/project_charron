package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanCargo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoadingPlanCargoServiceTest {

    private LoadingPlanCargoRepository cargoRepository;
    private LoadingPlanService loadingPlanService;
    private LoadingMovementLock movementLock;
    private LoadingPlanCargoService service;

    private LoadingPlan plan;

    private UUID planId;
    private UUID cargoId;
    private UUID movementId;

    @BeforeEach
    void setUp() {

        cargoRepository =
                mock(LoadingPlanCargoRepository.class);

        loadingPlanService =
                mock(LoadingPlanService.class);

        movementLock =
                mock(LoadingMovementLock.class);

        service = new LoadingPlanCargoService(
                cargoRepository,
                loadingPlanService,
                movementLock
        );

        movementId = UUID.randomUUID();
        cargoId = UUID.randomUUID();

        plan = LoadingPlan.create(
                movementId,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        planId = plan.id();

        when(loadingPlanService.getById(planId))
                .thenReturn(plan);

        when(movementLock.acquire(movementId))
                .thenReturn(true);
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

        verifyNoInteractions(movementLock);
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

        verifyNoInteractions(movementLock);
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
                service.updateQuantity(
                        planId,
                        existing.id(),
                        5
                );

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

    @Test
    void acquiresMovementLockBeforeAddingCargo() {

        LoadingPlanCargo result =
                service.addCargo(planId, cargoId, 3);

        InOrder order = inOrder(
                loadingPlanService,
                movementLock,
                cargoRepository
        );

        order.verify(loadingPlanService)
                .getById(planId);

        order.verify(movementLock)
                .acquire(movementId);

        order.verify(cargoRepository)
                .existsByLoadingPlanAndCargo(
                        planId,
                        cargoId
                );

        order.verify(cargoRepository)
                .insert(result);
    }

    @Test
    void acquiresMovementLockBeforeUpdatingQuantity() {

        LoadingPlanCargo existing =
                LoadingPlanCargo.create(planId, cargoId, 2);

        when(cargoRepository.findById(existing.id()))
                .thenReturn(Optional.of(existing));

        LoadingPlanCargo updated =
                service.updateQuantity(
                        planId,
                        existing.id(),
                        5
                );

        InOrder order = inOrder(
                loadingPlanService,
                movementLock,
                cargoRepository
        );

        order.verify(loadingPlanService)
                .getById(planId);

        order.verify(movementLock)
                .acquire(movementId);

        order.verify(cargoRepository)
                .findById(existing.id());

        order.verify(cargoRepository)
                .updateQuantity(updated);
    }

    @Test
    void acquiresMovementLockBeforeRemovingCargo() {

        LoadingPlanCargo line =
                LoadingPlanCargo.create(planId, cargoId, 2);

        when(cargoRepository.findById(line.id()))
                .thenReturn(Optional.of(line));

        service.removeCargo(planId, line.id());

        InOrder order = inOrder(
                loadingPlanService,
                movementLock,
                cargoRepository
        );

        order.verify(loadingPlanService)
                .getById(planId);

        order.verify(movementLock)
                .acquire(movementId);

        order.verify(cargoRepository)
                .findById(line.id());

        order.verify(cargoRepository)
                .deleteById(line.id());
    }

    @Test
    void retrievesCargoWithoutAcquiringMovementLock() {

        when(cargoRepository.findByLoadingPlanId(planId))
                .thenReturn(List.of());

        assertTrue(
                service.getCargo(planId).isEmpty()
        );

        verifyNoInteractions(movementLock);
    }

    @Test
    void rejectsCargoAdditionWhenMovementCannotBeLocked() {

        when(movementLock.acquire(movementId))
                .thenReturn(false);

        assertThrows(
                LoadingPlanNotFoundException.class,
                () -> service.addCargo(
                        planId,
                        cargoId,
                        3
                )
        );

        verifyNoInteractions(cargoRepository);
    }

    @Test
    void rejectsQuantityUpdateWhenMovementCannotBeLocked() {

        when(movementLock.acquire(movementId))
                .thenReturn(false);

        assertThrows(
                LoadingPlanNotFoundException.class,
                () -> service.updateQuantity(
                        planId,
                        UUID.randomUUID(),
                        5
                )
        );

        verifyNoInteractions(cargoRepository);
    }

    @Test
    void rejectsCargoRemovalWhenMovementCannotBeLocked() {

        when(movementLock.acquire(movementId))
                .thenReturn(false);

        assertThrows(
                LoadingPlanNotFoundException.class,
                () -> service.removeCargo(
                        planId,
                        UUID.randomUUID()
                )
        );

        verifyNoInteractions(cargoRepository);
    }

    @Test
    void rejectsInvalidQuantityBeforeAcquiringMovementLock() {

        assertThrows(
                InvalidLoadingPlanException.class,
                () -> service.updateQuantity(
                        planId,
                        UUID.randomUUID(),
                        0
                )
        );

        verifyNoInteractions(movementLock);
        verifyNoInteractions(cargoRepository);
    }
}