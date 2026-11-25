package com.charron.api.movement.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ManifestFreshnessComparatorTest {

    private final UUID movementId = UUID.randomUUID();

    private final Instant departure =
            Instant.parse("2026-12-01T08:00:00Z");

    private final Instant arrival =
            Instant.parse("2026-12-02T08:00:00Z");

    private final UUID loadingPlanId = UUID.randomUUID();

    private MovementManifest.EquipmentEntry equipment;
    private MovementManifest.CargoEntry cargo;
    private MovementManifest.LoadingEntry loading;

    private MovementManifest original;

    @BeforeEach
    void setUp() {

        equipment = new MovementManifest.EquipmentEntry(
                UUID.randomUUID(),
                "Test Tractor",
                MovementEquipmentRole.values()[0],
                "TEST-VIN"
        );

        cargo = new MovementManifest.CargoEntry(
                loadingPlanId,
                UUID.randomUUID(),
                "Test Cargo",
                10,
                1200L,
                1000L,
                800L,
                50000L
        );

        loading = new MovementManifest.LoadingEntry(
                loadingPlanId,
                equipment.equipmentId(),
                UUID.randomUUID(),
                "FEASIBLE",
                "{\"placements\":[]}"
        );

        original = createManifest(
                movementId,
                "MOV-001",
                "Denver, CO",
                "Dallas, TX",
                departure,
                arrival,
                List.of(equipment),
                List.of(cargo),
                List.of(loading)
        );
    }

    private MovementManifest createManifest(
            UUID movementId,
            String reference,
            String origin,
            String destination,
            Instant departure,
            Instant arrival,
            List<MovementManifest.EquipmentEntry> equipment,
            List<MovementManifest.CargoEntry> cargo,
            List<MovementManifest.LoadingEntry> loading
    ) {

        return new MovementManifest(
                UUID.randomUUID(),
                movementId,
                1,
                reference,
                origin,
                destination,
                departure,
                arrival,
                equipment,
                cargo,
                loading,
                Instant.now()
        );
    }

    private MovementManifest withContents(
            List<MovementManifest.EquipmentEntry> equipment,
            List<MovementManifest.CargoEntry> cargo,
            List<MovementManifest.LoadingEntry> loading
    ) {

        return createManifest(
                movementId,
                original.movementReference(),
                original.origin(),
                original.destination(),
                original.plannedDeparture(),
                original.plannedArrival(),
                equipment,
                cargo,
                loading
        );
    }

    private MovementManifest withMovementDetails(
            String reference,
            String origin,
            String destination,
            Instant departure,
            Instant arrival
    ) {

        return createManifest(
                movementId,
                reference,
                origin,
                destination,
                departure,
                arrival,
                original.equipment(),
                original.cargo(),
                original.loadingPlans()
        );
    }

    private void assertCurrent(
            ManifestFreshnessAssessment assessment
    ) {

        assertEquals(
                ManifestFreshnessStatus.CURRENT,
                assessment.status()
        );

        assertTrue(assessment.isCurrent());
        assertTrue(assessment.isVerifiable());
        assertFalse(assessment.isStale());
        assertTrue(assessment.changes().isEmpty());
        assertNull(assessment.reason());
        assertNotNull(assessment.assessedAt());
    }

    private void assertStale(
            ManifestFreshnessAssessment assessment,
            ManifestChangeType... expectedChanges
    ) {

        assertEquals(
                ManifestFreshnessStatus.STALE,
                assessment.status()
        );

        assertTrue(assessment.isStale());
        assertTrue(assessment.isVerifiable());
        assertFalse(assessment.isCurrent());

        assertEquals(
                Set.of(expectedChanges),
                assessment.changes()
        );

        assertNull(assessment.reason());
    }

    @Test
    void identicalManifestIsCurrent() {

        assertCurrent(
                ManifestFreshnessComparator.compare(
                        original,
                        original
                )
        );
    }

    @Test
    void ignoresGeneratedManifestMetadata() {

        MovementManifest regenerated =
                new MovementManifest(
                        UUID.randomUUID(),
                        original.movementId(),
                        5,
                        original.movementReference(),
                        original.origin(),
                        original.destination(),
                        original.plannedDeparture(),
                        original.plannedArrival(),
                        original.equipment(),
                        original.cargo(),
                        original.loadingPlans(),
                        original.generatedAt().plusSeconds(60)
                );

        assertCurrent(
                ManifestFreshnessComparator.compare(
                        original,
                        regenerated
                )
        );
    }

    @Test
    void assessmentReferencesOriginalManifest() {

        MovementManifest regenerated = withContents(
                original.equipment(),
                original.cargo(),
                original.loadingPlans()
        );

        ManifestFreshnessAssessment assessment =
                ManifestFreshnessComparator.compare(
                        original,
                        regenerated
                );

        assertEquals(
                original.id(),
                assessment.manifestId()
        );

        assertNotEquals(
                regenerated.id(),
                assessment.manifestId()
        );
    }

    @Test
    void detectsMovementReferenceChange() {

        MovementManifest current = withMovementDetails(
                "MOV-002",
                original.origin(),
                original.destination(),
                departure,
                arrival
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.MOVEMENT_REFERENCE
        );
    }

    @Test
    void detectsOriginChange() {

        MovementManifest current = withMovementDetails(
                original.movementReference(),
                "Colorado Springs, CO",
                original.destination(),
                departure,
                arrival
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.ORIGIN
        );
    }

    @Test
    void detectsDestinationChange() {

        MovementManifest current = withMovementDetails(
                original.movementReference(),
                original.origin(),
                "Houston, TX",
                departure,
                arrival
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.DESTINATION
        );
    }

    @Test
    void detectsDepartureChange() {

        MovementManifest current = withMovementDetails(
                original.movementReference(),
                original.origin(),
                original.destination(),
                departure.plusSeconds(3600),
                arrival
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.PLANNED_DEPARTURE
        );
    }

    @Test
    void detectsArrivalChange() {

        MovementManifest current = withMovementDetails(
                original.movementReference(),
                original.origin(),
                original.destination(),
                departure,
                arrival.plusSeconds(3600)
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.PLANNED_ARRIVAL
        );
    }

    @Test
    void detectsEquipmentReplacement() {

        MovementManifest.EquipmentEntry replacement =
                new MovementManifest.EquipmentEntry(
                        UUID.randomUUID(),
                        equipment.name(),
                        equipment.role(),
                        equipment.vin()
                );

        MovementManifest current = withContents(
                List.of(replacement),
                original.cargo(),
                original.loadingPlans()
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.EQUIPMENT
        );
    }

    @Test
    void detectsEquipmentRemoval() {

        MovementManifest current = withContents(
                List.of(),
                original.cargo(),
                original.loadingPlans()
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.EQUIPMENT
        );
    }

    @Test
    void detectsCargoQuantityChange() {

        MovementManifest.CargoEntry changed =
                new MovementManifest.CargoEntry(
                        cargo.loadingPlanId(),
                        cargo.cargoId(),
                        cargo.name(),
                        20,
                        cargo.lengthMm(),
                        cargo.widthMm(),
                        cargo.heightMm(),
                        cargo.unitWeightGrams()
                );

        MovementManifest current = withContents(
                original.equipment(),
                List.of(changed),
                original.loadingPlans()
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.CARGO
        );
    }

    @Test
    void detectsCargoWeightChange() {

        MovementManifest.CargoEntry changed =
                new MovementManifest.CargoEntry(
                        cargo.loadingPlanId(),
                        cargo.cargoId(),
                        cargo.name(),
                        cargo.quantity(),
                        cargo.lengthMm(),
                        cargo.widthMm(),
                        cargo.heightMm(),
                        75000L
                );

        MovementManifest current = withContents(
                original.equipment(),
                List.of(changed),
                original.loadingPlans()
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.CARGO
        );
    }

    @Test
    void detectsLoadingArrangementChange() {

        MovementManifest.LoadingEntry changed =
                new MovementManifest.LoadingEntry(
                        loading.loadingPlanId(),
                        loading.equipmentId(),
                        loading.cargoSpaceId(),
                        loading.arrangementStatus(),
                        "{\"placements\":[{\"x\":100}]}"
                );

        MovementManifest current = withContents(
                original.equipment(),
                original.cargo(),
                List.of(changed)
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.LOADING_PLANS
        );
    }

    @Test
    void detectsLoadingPlanRemoval() {

        MovementManifest current = withContents(
                original.equipment(),
                original.cargo(),
                List.of()
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.LOADING_PLANS
        );
    }

    @Test
    void ignoresCollectionOrdering() {

        MovementManifest.EquipmentEntry secondEquipment =
                new MovementManifest.EquipmentEntry(
                        UUID.randomUUID(),
                        "Second Equipment",
                        equipment.role(),
                        null
                );

        MovementManifest.CargoEntry secondCargo =
                new MovementManifest.CargoEntry(
                        loadingPlanId,
                        UUID.randomUUID(),
                        "Second Cargo",
                        5,
                        100L,
                        100L,
                        100L,
                        1000L
                );

        MovementManifest.LoadingEntry secondLoading =
                new MovementManifest.LoadingEntry(
                        UUID.randomUUID(),
                        secondEquipment.equipmentId(),
                        UUID.randomUUID(),
                        "FEASIBLE",
                        "{\"placements\":[]}"
                );

        MovementManifest first = withContents(
                List.of(equipment, secondEquipment),
                List.of(cargo, secondCargo),
                List.of(loading, secondLoading)
        );

        MovementManifest reordered = withContents(
                List.of(secondEquipment, equipment),
                List.of(secondCargo, cargo),
                List.of(secondLoading, loading)
        );

        assertCurrent(
                ManifestFreshnessComparator.compare(
                        first,
                        reordered
                )
        );
    }

    @Test
    void detectsDuplicateEntryCountChanges() {

        MovementManifest current = withContents(
                original.equipment(),
                List.of(cargo, cargo),
                original.loadingPlans()
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.CARGO
        );
    }

    @Test
    void detectsDifferentFrequenciesWithEqualCollectionSizes() {

        MovementManifest.CargoEntry secondCargo =
                new MovementManifest.CargoEntry(
                        loadingPlanId,
                        UUID.randomUUID(),
                        "Second Cargo",
                        5,
                        100L,
                        100L,
                        100L,
                        1000L
                );

        MovementManifest first = withContents(
                original.equipment(),
                List.of(cargo, cargo, secondCargo),
                original.loadingPlans()
        );

        MovementManifest second = withContents(
                original.equipment(),
                List.of(cargo, secondCargo, secondCargo),
                original.loadingPlans()
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        first,
                        second
                ),
                ManifestChangeType.CARGO
        );
    }

    @Test
    void detectsMultipleOperationalChanges() {

        MovementManifest current = createManifest(
                movementId,
                original.movementReference(),
                original.origin(),
                "Houston, TX",
                departure.plusSeconds(3600),
                arrival,
                List.of(),
                List.of(),
                List.of()
        );

        assertStale(
                ManifestFreshnessComparator.compare(
                        original,
                        current
                ),
                ManifestChangeType.DESTINATION,
                ManifestChangeType.PLANNED_DEPARTURE,
                ManifestChangeType.EQUIPMENT,
                ManifestChangeType.CARGO,
                ManifestChangeType.LOADING_PLANS
        );
    }

    @Test
    void rejectsComparisonAcrossDifferentMovements() {

        MovementManifest unrelated = createManifest(
                UUID.randomUUID(),
                original.movementReference(),
                original.origin(),
                original.destination(),
                departure,
                arrival,
                original.equipment(),
                original.cargo(),
                original.loadingPlans()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ManifestFreshnessComparator.compare(
                        original,
                        unrelated
                )
        );
    }

    @Test
    void rejectsNullPersistedManifest() {

        assertThrows(
                NullPointerException.class,
                () -> ManifestFreshnessComparator.compare(
                        null,
                        original
                )
        );
    }

    @Test
    void rejectsNullCurrentManifest() {

        assertThrows(
                NullPointerException.class,
                () -> ManifestFreshnessComparator.compare(
                        original,
                        null
                )
        );
    }
}