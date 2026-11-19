
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EquipmentRegistrationServiceTest {

    private static final String TEST_VIN =
            "1HGCM82633A004352";

    @Autowired
    private EquipmentRegistrationService registrationService;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Test
    void registersEquipmentAndCompleteVinSnapshot() {
        Equipment equipment = registrationService.register(
                "Honda Accord Registration Test",
                EquipmentCategory.OTHER,
                TEST_VIN
        );

        try {
            Integer equipmentCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM equipment
                    WHERE id = ?
                    """,
                    Integer.class,
                    equipment.getId()
            );

            Integer snapshotCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM equipment_vin_decodes
                    WHERE equipment_id = ?
                    """,
                    Integer.class,
                    equipment.getId()
            );

            Integer rawRowCount = jdbcTemplate.queryForObject("""
                    SELECT jsonb_array_length(raw_response)
                    FROM equipment_vin_decodes
                    WHERE equipment_id = ?
                    """,
                    Integer.class,
                    equipment.getId()
            );

            assertEquals(1, equipmentCount);
            assertEquals(1, snapshotCount);
            assertEquals(44, rawRowCount);

        } finally {
            // Clean up the committed test registration.
            jdbcTemplate.update(
                    "DELETE FROM equipment_vin_decodes WHERE equipment_id = ?",
                    equipment.getId()
            );

            jdbcTemplate.update(
                    "DELETE FROM equipment WHERE id = ?",
                    equipment.getId()
            );
        }
    }

    @Test
    void doesNotRegisterEquipmentWhenVinDecodeFails() {
        String invalidVin = "INVALIDVIN1234567";

        assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(
                        "Failed VIN Registration Test",
                        EquipmentCategory.OTHER,
                        invalidVin
                )
        );

        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM equipment
                WHERE name = 'Failed VIN Registration Test'
                """,
                Integer.class
        );

        assertEquals(0, count);
    }
}
