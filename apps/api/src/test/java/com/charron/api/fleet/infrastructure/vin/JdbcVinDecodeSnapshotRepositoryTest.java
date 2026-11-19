
package com.charron.api.fleet.infrastructure.vin;

import com.charron.api.fleet.application.VinDecodeSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JdbcVinDecodeSnapshotRepositoryTest {

    @Autowired
    private JdbcVinDecodeSnapshotRepository repository;

    @Autowired
    private PostgresVehicleVinDecoder decoder;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesCompleteVinDecodeSnapshot() {
        UUID equipmentId = UUID.randomUUID();

        // Create a temporary equipment identity.
        jdbcTemplate.update("""
                INSERT INTO equipment (id, name, category, vin)
                VALUES (?, ?, ?, ?)
                """,
                equipmentId,
                "2003 Honda Accord Test",
                "OTHER",
                "1HGCM82633A004352"
        );

        // Decode using the actual local vPIC database.
        VinDecodeSnapshot snapshot =
                decoder.decodeSnapshot("1HGCM82633A004352");

        assertEquals(44, snapshot.rawResponse().size());

        // Persist to CHARRON's operational database.
        repository.save(equipmentId, snapshot);

        Integer snapshotCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM equipment_vin_decodes
                WHERE equipment_id = ?
                """,
                Integer.class,
                equipmentId
        );

        assertEquals(1, snapshotCount);

        Integer rawRowCount = jdbcTemplate.queryForObject("""
                SELECT jsonb_array_length(raw_response)
                FROM equipment_vin_decodes
                WHERE equipment_id = ?
                """,
                Integer.class,
                equipmentId
        );

        assertEquals(44, rawRowCount);

        String engineModel = jdbcTemplate.queryForObject("""
                SELECT decoded_vehicle ->> 'engineModel'
                FROM equipment_vin_decodes
                WHERE equipment_id = ?
                """,
                String.class,
                equipmentId
        );

        assertEquals("J30A4", engineModel);


        Integer engineMetadataCount = jdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM equipment_vin_decodes d,
             jsonb_array_elements(d.raw_response) AS attribute
        WHERE d.equipment_id = ?
          AND attribute ->> 'code' = 'EngineModel'
          AND jsonb_exists(attribute, 'itempatternid')
          AND jsonb_exists(attribute, 'itemsource')
        """,
                Integer.class,
                equipmentId
        );

        assertEquals(1, engineMetadataCount);


        String decoderSource = jdbcTemplate.queryForObject("""
                SELECT decoder_source
                FROM equipment_vin_decodes
                WHERE equipment_id = ?
                """,
                String.class,
                equipmentId
        );

        assertEquals("NHTSA_VPIC_LOCAL", decoderSource);
    }


    @Test
    void retrievesLatestCompleteVinDecodeSnapshot() {
        UUID equipmentId = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO equipment (id, name, category, vin)
            VALUES (?, ?, ?, ?)
            """,
                equipmentId,
                "VIN Retrieval Test",
                "OTHER",
                "1HGCM82633A004352"
        );

        VinDecodeSnapshot original =
                decoder.decodeSnapshot("1HGCM82633A004352");

        repository.save(equipmentId, original);

        var retrieved =
                repository.findLatestByEquipmentId(equipmentId);

        assertTrue(retrieved.isPresent());

        VinDecodeSnapshot snapshot = retrieved.orElseThrow();

        // Verify normalized vehicle information.
        assertEquals(
                "1HGCM82633A004352",
                snapshot.decodedVehicle().vin()
        );

        assertEquals(
                "J30A4",
                snapshot.decodedVehicle().engineModel()
        );

        assertEquals(
                6,
                snapshot.decodedVehicle().engineCylinders()
        );

        // Verify the complete raw response.
        assertEquals(44, snapshot.rawResponse().size());

        assertEquals(
                original.rawResponse().size(),
                snapshot.rawResponse().size()
        );

        assertTrue(snapshot.rawResponse().stream().anyMatch(row ->
                "EngineModel".equals(row.get("code"))
                        && "J30A4".equals(row.get("value"))
                        && row.containsKey("itempatternid")
                        && row.containsKey("itemsource")
        ));

        // Unknown and unpopulated attributes remain present.
        assertTrue(snapshot.rawResponse().stream().anyMatch(row ->
                row.get("value") == null
                        || row.get("value").toString().isBlank()
        ));
    }

}
