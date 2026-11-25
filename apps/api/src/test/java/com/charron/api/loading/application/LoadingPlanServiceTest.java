package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoadingPlanServiceTest {

    private LoadingPlanRepository repository;
    private CargoSpaceOwnershipChecker ownershipChecker;
    private LoadingPlanEligibilityChecker eligibilityChecker;
    private LoadingMovementLock movementLock;
    private LoadingPlanService service;

    private UUID movementId;
    private UUID equipmentId;
    private UUID cargoSpaceId;

    @BeforeEach
    void setUp() {

        repository = mock(LoadingPlanRepository.class);

        ownershipChecker =
                mock(CargoSpaceOwnershipChecker.class);

        eligibilityChecker =
                mock(LoadingPlanEligibilityChecker.class);

        movementLock =
                mock(LoadingMovementLock.class);

        service = new LoadingPlanService(
                repository,
                ownershipChecker,
                eligibilityChecker,
                movementLock
        );

        movementId = UUID.randomUUID();
        equipmentId = UUID.randomUUID();
        cargoSpaceId = UUID.randomUUID();

        when(movementLock.acquire(movementId))
                .thenReturn(true);

        when(eligibilityChecker.movementExists(movementId))
                .thenReturn(true);

        when(eligibilityChecker.equipmentAssignedToMovement(
                movementId, equipmentId
        )).thenReturn(true);

        when(ownershipChecker.belongsToEquipment(
                cargoSpaceId, equipmentId
        )).thenReturn(true);
    }

    @Test
    void createsEligibleLoadingPlan() {

        LoadingPlan plan = service.create(
                movementId, equipmentId, cargoSpaceId
        );

        assertNotNull(plan.id());
        assertEquals(movementId, plan.movementId());
        assertEquals(equipmentId, plan.equipmentId());
        assertEquals(cargoSpaceId, plan.cargoSpaceId());

        verify(repository).insert(plan);
    }

    @Test
    void rejectsUnknownMovement() {

        when(movementLock.acquire(movementId))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        movementId, equipmentId, cargoSpaceId
                )
        );

        verify(repository, never()).insert(any());
        verifyNoInteractions(ownershipChecker);
        verifyNoInteractions(eligibilityChecker);
    }

    @Test
    void rejectsUnassignedEquipment() {

        when(eligibilityChecker.equipmentAssignedToMovement(
                movementId, equipmentId
        )).thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> service.create(
                        movementId, equipmentId, cargoSpaceId
                )
        );

        verify(repository, never()).insert(any());
    }

    @Test
    void rejectsCargoSpaceOwnedByOtherEquipment() {

        when(ownershipChecker.belongsToEquipment(
                cargoSpaceId, equipmentId
        )).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        movementId, equipmentId, cargoSpaceId
                )
        );

        verify(repository, never()).insert(any());
    }

    @Test
    void rejectsDuplicateMovementCargoSpace() {

        when(repository.existsByMovementAndCargoSpace(
                movementId, cargoSpaceId
        )).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> service.create(
                        movementId, equipmentId, cargoSpaceId
                )
        );

        verify(repository, never()).insert(any());
    }

    @Test
    void retrievesExistingPlan() {

        LoadingPlan plan = LoadingPlan.create(
                movementId, equipmentId, cargoSpaceId
        );

        when(repository.findById(plan.id()))
                .thenReturn(Optional.of(plan));

        assertEquals(
                plan,
                service.getById(plan.id())
        );
    }

    @Test
    void rejectsUnknownPlan() {

        UUID unknownId = UUID.randomUUID();

        when(repository.findById(unknownId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getById(unknownId)
        );
    }

    @Test
    void listsPlansForMovement() {

        LoadingPlan plan = LoadingPlan.create(
                movementId, equipmentId, cargoSpaceId
        );

        when(repository.findByMovementId(movementId))
                .thenReturn(List.of(plan));

        assertEquals(
                List.of(plan),
                service.getByMovementId(movementId)
        );
    }

    @Test
    void rejectsListingForUnknownMovement() {

        when(eligibilityChecker.movementExists(movementId))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getByMovementId(movementId)
        );

        verify(repository, never())
                .findByMovementId(any());
    }

    @Test
    void acquiresMovementLockBeforeValidationAndInsertion() {

        LoadingPlan plan = service.create(
                movementId,
                equipmentId,
                cargoSpaceId
        );

        InOrder order = inOrder(
                movementLock,
                eligibilityChecker,
                ownershipChecker,
                repository
        );

        order.verify(movementLock)
                .acquire(movementId);

        order.verify(eligibilityChecker)
                .equipmentAssignedToMovement(
                        movementId,
                        equipmentId
                );

        order.verify(ownershipChecker)
                .belongsToEquipment(
                        cargoSpaceId,
                        equipmentId
                );

        order.verify(repository)
                .existsByMovementAndCargoSpace(
                        movementId,
                        cargoSpaceId
                );

        order.verify(repository)
                .insert(plan);
    }

    @Test
    void retrievesPlanWithoutAcquiringMovementLock() {

        LoadingPlan plan = LoadingPlan.create(
                movementId,
                equipmentId,
                cargoSpaceId
        );

        when(repository.findById(plan.id()))
                .thenReturn(Optional.of(plan));

        assertEquals(
                plan,
                service.getById(plan.id())
        );

        verifyNoInteractions(movementLock);
    }

    @Test
    void listsPlansWithoutAcquiringMovementLock() {

        when(repository.findByMovementId(movementId))
                .thenReturn(List.of());

        assertTrue(
                service.getByMovementId(movementId)
                        .isEmpty()
        );

        verifyNoInteractions(movementLock);
    }

    @Test
    void rejectsInvalidIdentifiersBeforeAcquiringLock() {

        assertThrows(
                InvalidLoadingPlanException.class,
                () -> service.create(
                        null,
                        equipmentId,
                        cargoSpaceId
                )
        );

        assertThrows(
                InvalidLoadingPlanException.class,
                () -> service.create(
                        movementId,
                        null,
                        cargoSpaceId
                )
        );

        assertThrows(
                InvalidLoadingPlanException.class,
                () -> service.create(
                        movementId,
                        equipmentId,
                        null
                )
        );

        verifyNoInteractions(movementLock);
        verifyNoInteractions(repository);
    }
}