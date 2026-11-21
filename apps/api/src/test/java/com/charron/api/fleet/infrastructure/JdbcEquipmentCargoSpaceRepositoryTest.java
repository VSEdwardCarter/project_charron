
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.application.EquipmentCargoSpaceRepository;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;
import com.charron.api.fleet.domain.CargoSpaceType;
import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JdbcEquipmentCargoSpaceRepositoryTest {

    @Autowired
    private EquipmentCargoSpaceRepository cargoSpaceRepository;

    @Autowired
    private JdbcEquipmentRepository equipmentRepository;

    @Test
    void savesAndRetrievesEnclosedTrailerCargoSpace() {
        UUID equipmentId = UUID.randomUUID();
        UUID cargoSpaceId = UUID.randomUUID();

        Equipment trailer = new Equipment(
                equipmentId,
                "Test Enclosed Trailer",
                EquipmentCategory.TRAILER,
                null
        );

        equipmentRepository.insertIdentity(trailer);

        CargoSpaceSpecifications specifications =
                new CargoSpaceSpecifications(
                        CargoSpaceType.ENCLOSED,
                        7000,
                        2400,
                        2500L
                );

        cargoSpaceRepository.insert(
                cargoSpaceId,
                equipmentId,
                "Main Cargo Compartment",
                specifications
        );

        var spaces =
                cargoSpaceRepository.findByEquipmentId(equipmentId);

        assertEquals(1, spaces.size());

        var saved = spaces.getFirst();

        assertEquals(cargoSpaceId, saved.id());
        assertEquals(equipmentId, saved.equipmentId());
        assertEquals("Main Cargo Compartment", saved.name());
        assertEquals(specifications, saved.specifications());
    }

    @Test
    void supportsOpenDeckWithoutDefinedHeight() {
        UUID equipmentId = UUID.randomUUID();

        equipmentRepository.insertIdentity(
                new Equipment(
                        equipmentId,
                        "Test Flatbed Trailer",
                        EquipmentCategory.TRAILER,
                        null
                )
        );

        cargoSpaceRepository.insert(
                UUID.randomUUID(),
                equipmentId,
                "Flatbed Deck",
                new CargoSpaceSpecifications(
                        CargoSpaceType.OPEN_DECK,
                        12000,
                        2500,
                        null
                )
        );

        var spaces =
                cargoSpaceRepository.findByEquipmentId(equipmentId);

        assertEquals(1, spaces.size());
        assertEquals(
                CargoSpaceType.OPEN_DECK,
                spaces.getFirst().specifications().type()
        );
        assertNull(
                spaces.getFirst().specifications().usableHeightMm()
        );
    }

    @Test
    void isolatesCargoSpacesByEquipment() {
        UUID firstEquipmentId = UUID.randomUUID();
        UUID secondEquipmentId = UUID.randomUUID();

        equipmentRepository.insertIdentity(
                new Equipment(
                        firstEquipmentId,
                        "Trailer A",
                        EquipmentCategory.TRAILER,
                        null
                )
        );

        equipmentRepository.insertIdentity(
                new Equipment(
                        secondEquipmentId,
                        "Trailer B",
                        EquipmentCategory.TRAILER,
                        null
                )
        );

        cargoSpaceRepository.insert(
                UUID.randomUUID(),
                firstEquipmentId,
                "Cargo Space A",
                new CargoSpaceSpecifications(
                        CargoSpaceType.ENCLOSED,
                        7000,
                        2400,
                        2500L
                )
        );

        assertEquals(
                1,
                cargoSpaceRepository.findByEquipmentId(
                        firstEquipmentId
                ).size()
        );

        assertTrue(
                cargoSpaceRepository.findByEquipmentId(
                        secondEquipmentId
                ).isEmpty()
        );
    }

    @Test
    void rejectsMissingCargoSpaceName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> cargoSpaceRepository.insert(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        " ",
                        new CargoSpaceSpecifications(
                                CargoSpaceType.ENCLOSED,
                                7000,
                                2400,
                                2500L
                        )
                )
        );
    }


    @Test
    void findByIdReturnsEmptyForUnknownCargoSpace() {
        var result = cargoSpaceRepository.findById(
                UUID.randomUUID()
        );

        assertTrue(result.isEmpty());
    }


    @Test
    void findsCargoSpaceByIdForCargoVan() {
        UUID equipmentId = UUID.randomUUID();
        UUID cargoSpaceId = UUID.randomUUID();

        Equipment van = new Equipment(
                equipmentId,
                "Test Cargo Van",
                EquipmentCategory.CARGO_VAN,
                null
        );

        equipmentRepository.insertIdentity(van);

        CargoSpaceSpecifications specifications =
                new CargoSpaceSpecifications(
                        CargoSpaceType.ENCLOSED,
                        3200,
                        1700,
                        1800L
                );

        cargoSpaceRepository.insert(
                cargoSpaceId,
                equipmentId,
                "Rear Cargo Compartment",
                specifications
        );

        var result = cargoSpaceRepository.findById(cargoSpaceId);

        assertTrue(result.isPresent());

        var saved = result.orElseThrow();

        assertAll(
                () -> assertEquals(cargoSpaceId, saved.id()),
                () -> assertEquals(equipmentId, saved.equipmentId()),
                () -> assertEquals(
                        "Rear Cargo Compartment",
                        saved.name()
                ),
                () -> assertEquals(
                        specifications,
                        saved.specifications()
                )
        );
    }


}


