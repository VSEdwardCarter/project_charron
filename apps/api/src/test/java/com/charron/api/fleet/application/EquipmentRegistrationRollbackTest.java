
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.EquipmentCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class EquipmentRegistrationRollbackTest {

    private static final String TEST_VIN =
            "1HGCM82633A004352";

    @Autowired
    private EquipmentRegistrationService registrationService;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private VinDecodeSnapshotRepository snapshotRepository;

    @Test
    void rollsBackEquipmentWhenSnapshotPersistenceFails() {
        String equipmentName =
                "Rollback Test " + UUID.randomUUID();

        // Simulate a failure after equipment identity insertion.
        doThrow(new IllegalStateException(
                "Simulated VIN snapshot persistence failure"
        )).when(snapshotRepository).save(any(), any());

        assertThrows(
                IllegalStateException.class,
                () -> registrationService.register(
                        equipmentName,
                        EquipmentCategory.OTHER,
                        TEST_VIN
                )
        );

        // The equipment identity must not remain in the database.
        Integer equipmentCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM equipment
                WHERE name = ?
                """,
                Integer.class,
                equipmentName
        );

        assertEquals(0, equipmentCount);
    }
}
