
package com.charron.api.cargo.web;

import com.charron.api.cargo.application.CargoRepository;
import com.charron.api.cargo.domain.Cargo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CargoControllerTest {

    private final CargoRepository repository =
            mock(CargoRepository.class);

    private final CargoController controller =
            new CargoController(repository);

    @Test
    void registersCargoWithCompleteMeasurements() {
        var request = new CargoRegistrationRequest(
                "Generator",
                "GEN-001",
                "Portable generator",
                150000L,
                1200L,
                800L,
                1000L
        );

        CargoResponse response = controller.register(request);

        assertNotNull(response.id());
        assertEquals("Generator", response.name());
        assertEquals(150000L, response.weightGrams());
        assertEquals(1200L, response.lengthMm());
        assertEquals(800L, response.widthMm());
        assertEquals(1000L, response.heightMm());

        verify(repository).insert(any(Cargo.class));
    }

    @Test
    void registersCargoWithoutMeasurements() {
        var request = new CargoRegistrationRequest(
                "Unmeasured Cargo",
                null,
                null,
                null,
                null,
                null,
                null
        );

        CargoResponse response = controller.register(request);

        assertNull(response.weightGrams());
        assertNull(response.lengthMm());
        assertNull(response.widthMm());
        assertNull(response.heightMm());

        verify(repository).insert(any(Cargo.class));
    }

    @Test
    void rejectsPartialDimensions() {
        var request = new CargoRegistrationRequest(
                "Invalid Cargo",
                null,
                null,
                null,
                1200L,
                null,
                1000L
        );

        var exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.register(request)
        );

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsZeroWeight() {
        var request = new CargoRegistrationRequest(
                "Invalid Weight",
                null,
                null,
                0L,
                null,
                null,
                null
        );

        var exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.register(request)
        );

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(repository);
    }

    @Test
    void listsRegisteredCargo() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Test Cargo",
                null,
                null
        );

        when(repository.findAll()).thenReturn(List.of(cargo));

        List<CargoResponse> response = controller.findAll();

        assertEquals(1, response.size());
        assertEquals(cargo.getId(), response.getFirst().id());
    }

    @Test
    void returnsCargoById() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Test Cargo",
                null,
                null
        );

        when(repository.findById(cargo.getId()))
                .thenReturn(Optional.of(cargo));

        CargoResponse response = controller.findById(cargo.getId());

        assertEquals(cargo.getId(), response.id());
    }

    @Test
    void returnsNotFoundForUnknownCargo() {
        UUID unknownId = UUID.randomUUID();

        when(repository.findById(unknownId))
                .thenReturn(Optional.empty());

        var exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.findById(unknownId)
        );

        assertEquals(404, exception.getStatusCode().value());
    }
}
