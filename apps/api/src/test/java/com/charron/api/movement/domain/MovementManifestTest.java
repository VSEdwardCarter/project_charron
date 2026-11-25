
package com.charron.api.movement.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MovementManifestTest {

    private final UUID movementId = UUID.randomUUID();
    private final UUID equipmentId = UUID.randomUUID();
    private final UUID loadingPlanId = UUID.randomUUID();
    private final UUID cargoSpaceId = UUID.randomUUID();

    private MovementManifest.EquipmentEntry equipment() {
        return new MovementManifest.EquipmentEntry(
                equipmentId,
                "2022 Big Tex Trailer",
                MovementEquipmentRole.CARGO_CARRIER,
                null
        );
    }

    private MovementManifest.CargoEntry cargo() {
        return new MovementManifest.CargoEntry(
                loadingPlanId,
                UUID.randomUUID(),
                "Palletized Freight",
                4,
                1200L,
                1000L,
                1500L,
                250_000L
        );
    }

    private MovementManifest.LoadingEntry loading() {
        return new MovementManifest.LoadingEntry(
                loadingPlanId,
                equipmentId,
                cargoSpaceId,
                "VALID",
                "{\"status\":\"VALID\",\"placements\":[]}"
        );
    }

    private MovementManifest manifest(
            int version,
            List<MovementManifest.EquipmentEntry> equipment,
            List<MovementManifest.CargoEntry> cargo,
            List<MovementManifest.LoadingEntry> loading
    ) {
        return new MovementManifest(
                UUID.randomUUID(),
                movementId,
                version,
                "MOV-002",
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z"),
                equipment,
                cargo,
                loading,
                Instant.parse("2026-11-23T20:00:00Z")
        );
    }

    @Test
    void createsManifestWithExpectedDetails() {
        MovementManifest result = manifest(
                1,
                List.of(equipment()),
                List.of(cargo()),
                List.of(loading())
        );

        assertEquals(movementId, result.movementId());
        assertEquals("MOV-002", result.movementReference());
        assertEquals("Denver, CO", result.origin());
        assertEquals("Dallas, TX", result.destination());
        assertEquals(1, result.version());
        assertEquals(1, result.equipment().size());
        assertEquals(1, result.cargo().size());
        assertEquals(1, result.loadingPlans().size());
    }

    @Test
    void rejectsInvalidManifestVersion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> manifest(
                        0,
                        List.of(),
                        List.of(),
                        List.of()
                )
        );
    }

    @Test
    void rejectsNullMovementId() {
        assertThrows(
                NullPointerException.class,
                () -> new MovementManifest(
                        UUID.randomUUID(),
                        null,
                        1,
                        "MOV-002",
                        "Denver",
                        "Dallas",
                        Instant.parse("2026-12-01T08:00:00Z"),
                        Instant.parse("2026-12-02T08:00:00Z"),
                        List.of(),
                        List.of(),
                        List.of(),
                        Instant.now()
                )
        );
    }

    @Test
    void manifestCollectionsAreImmutable() {
        List<MovementManifest.EquipmentEntry> source =
                new ArrayList<>();

        source.add(equipment());

        MovementManifest result = manifest(
                1,
                source,
                List.of(cargo()),
                List.of(loading())
        );

        source.clear();

        assertEquals(1, result.equipment().size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.equipment().clear()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.cargo().clear()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.loadingPlans().clear()
        );
    }

    @Test
    void calculatesTotalCargoWeight() {
        MovementManifest.CargoEntry entry = cargo();

        assertEquals(
                1_000_000L,
                entry.totalWeightGrams()
        );
    }

    @Test
    void unknownUnitWeightProducesUnknownTotal() {
        MovementManifest.CargoEntry entry =
                new MovementManifest.CargoEntry(
                        loadingPlanId,
                        UUID.randomUUID(),
                        "Unweighed Freight",
                        2,
                        null,
                        null,
                        null,
                        null
                );

        assertNull(entry.totalWeightGrams());
    }

    @Test
    void rejectsNonPositiveCargoQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MovementManifest.CargoEntry(
                        loadingPlanId,
                        UUID.randomUUID(),
                        "Freight",
                        0,
                        1000L,
                        1000L,
                        1000L,
                        500L
                )
        );
    }

    @Test
    void detectsCargoWeightOverflow() {
        MovementManifest.CargoEntry entry =
                new MovementManifest.CargoEntry(
                        loadingPlanId,
                        UUID.randomUUID(),
                        "Heavy Freight",
                        Integer.MAX_VALUE,
                        null,
                        null,
                        null,
                        Long.MAX_VALUE
                );

        assertThrows(
                ArithmeticException.class,
                entry::totalWeightGrams
        );
    }

    @Test
    void preservesLoadingArrangementSnapshot() {
        String snapshot =
                "{\"status\":\"VALID\",\"placements\":[{\"xMm\":100}]}";

        MovementManifest.LoadingEntry entry =
                new MovementManifest.LoadingEntry(
                        loadingPlanId,
                        equipmentId,
                        cargoSpaceId,
                        "VALID",
                        snapshot
                );

        assertEquals(snapshot, entry.arrangementSnapshot());
    }

    @Test
    void supportsManifestVersioning() {
        MovementManifest first = manifest(
                1,
                List.of(equipment()),
                List.of(cargo()),
                List.of(loading())
        );

        MovementManifest revised = manifest(
                2,
                List.of(equipment()),
                List.of(cargo()),
                List.of(loading())
        );

        assertEquals(1, first.version());
        assertEquals(2, revised.version());
        assertNotEquals(first.id(), revised.id());
    }
}
