
package com.charron.api.fleet.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CargoSpaceSpecificationsTest {

    @Test
    void createsEnclosedCargoSpace() {
        CargoSpaceSpecifications space =
                new CargoSpaceSpecifications(
                        CargoSpaceType.ENCLOSED,
                        7000,
                        2400,
                        2500L
                );

        assertEquals(CargoSpaceType.ENCLOSED, space.type());
        assertTrue(space.hasDefinedHeight());

        assertEquals(7000, space.usableDimensions().lengthMm());
        assertEquals(2400, space.usableDimensions().widthMm());
        assertEquals(2500, space.usableDimensions().heightMm());
    }

    @Test
    void createsOpenDeckWithoutHeight() {
        CargoSpaceSpecifications space =
                new CargoSpaceSpecifications(
                        CargoSpaceType.OPEN_DECK,
                        12000,
                        2500,
                        null
                );

        assertFalse(space.hasDefinedHeight());

        assertThrows(
                IllegalStateException.class,
                space::usableDimensions
        );
    }

    @Test
    void allowsOpenDeckWithDefinedHeightLimit() {
        CargoSpaceSpecifications space =
                new CargoSpaceSpecifications(
                        CargoSpaceType.OPEN_DECK,
                        12000,
                        2500,
                        4000L
                );

        assertTrue(space.hasDefinedHeight());
        assertEquals(4000, space.usableDimensions().heightMm());
    }

    @Test
    void rejectsEnclosedSpaceWithoutHeight() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CargoSpaceSpecifications(
                        CargoSpaceType.ENCLOSED,
                        7000,
                        2400,
                        null
                )
        );
    }

    @Test
    void rejectsNonPositiveMeasurements() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CargoSpaceSpecifications(
                        CargoSpaceType.ENCLOSED,
                        0,
                        2400,
                        2500L
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new CargoSpaceSpecifications(
                        CargoSpaceType.OPEN_DECK,
                        12000,
                        -1,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new CargoSpaceSpecifications(
                        CargoSpaceType.ENCLOSED,
                        7000,
                        2400,
                        0L
                )
        );
    }

    @Test
    void rejectsMissingCargoSpaceType() {
        assertThrows(
                NullPointerException.class,
                () -> new CargoSpaceSpecifications(
                        null,
                        7000,
                        2400,
                        2500L
                )
        );
    }
}
