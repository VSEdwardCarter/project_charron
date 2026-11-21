
package com.charron.api.integration.infrastructure;

import com.charron.api.integration.application.ExternalSourceRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JdbcExternalSourceRecordRepositoryTest {

    @Autowired
    private ExternalSourceRecordRepository repository;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private String newExternalId() {
        return UUID.randomUUID().toString();
    }

    @Test
    void insertsAndRetrievesSourceRecord() {
        String externalId = newExternalId();

        Map<String, Object> payload = Map.of(
                "product_id", externalId,
                "product_category_name", "automotivo",
                "product_weight_g", 225
        );

        repository.upsert(
                "olist", "product", externalId, payload
        );

        assertEquals(
                payload,
                repository.findPayload(
                        "olist", "product", externalId
                ).orElseThrow()
        );

        assertEquals(
                1L,
                repository.count("olist", "product")
        );
    }

    @Test
    void identicalReplayPreservesRecordAndTimestamps() {
        String externalId = newExternalId();

        Map<String, Object> payload = Map.of(
                "product_id", externalId,
                "product_weight_g", 225
        );

        repository.upsert(
                "olist", "product", externalId, payload
        );

        UUID originalId = findRecordId(externalId);

        Instant historicalTime =
                Instant.parse("2020-01-01T00:00:00Z");

        jdbcTemplate.update("""
                UPDATE external_source_records
                SET first_ingested_at = ?,
                    last_ingested_at = ?
                WHERE id = ?
                """,
                Timestamp.from(historicalTime),
                Timestamp.from(historicalTime),
                originalId
        );

        repository.upsert(
                "olist", "product", externalId, payload
        );

        assertEquals(originalId, findRecordId(externalId));

        assertEquals(
                historicalTime,
                findTimestamp(externalId, "first_ingested_at")
        );

        assertEquals(
                historicalTime,
                findTimestamp(externalId, "last_ingested_at")
        );

        assertEquals(
                1L,
                repository.count("olist", "product")
        );
    }

    @Test
    void changedPayloadUpdatesExistingRecord() {
        String externalId = newExternalId();

        repository.upsert(
                "olist",
                "product",
                externalId,
                Map.of("product_weight_g", 225)
        );

        UUID originalId = findRecordId(externalId);

        Instant historicalTime =
                Instant.parse("2020-01-01T00:00:00Z");

        jdbcTemplate.update("""
                UPDATE external_source_records
                SET first_ingested_at = ?,
                    last_ingested_at = ?
                WHERE id = ?
                """,
                Timestamp.from(historicalTime),
                Timestamp.from(historicalTime),
                originalId
        );

        repository.upsert(
                "olist",
                "product",
                externalId,
                Map.of("product_weight_g", 300)
        );

        assertEquals(originalId, findRecordId(externalId));

        assertEquals(
                300,
                repository.findPayload(
                        "olist", "product", externalId
                ).orElseThrow().get("product_weight_g")
        );

        assertEquals(
                historicalTime,
                findTimestamp(externalId, "first_ingested_at")
        );

        assertTrue(
                findTimestamp(
                        externalId, "last_ingested_at"
                ).isAfter(historicalTime)
        );

        assertEquals(
                1L,
                repository.count("olist", "product")
        );
    }

    @Test
    void separatesRecordsBySourceSystem() {
        String externalId = newExternalId();

        repository.upsert(
                "olist",
                "product",
                externalId,
                Map.of("source", "olist")
        );

        repository.upsert(
                "partner_b",
                "product",
                externalId,
                Map.of("source", "partner_b")
        );

        assertEquals(
                "olist",
                repository.findPayload(
                        "olist", "product", externalId
                ).orElseThrow().get("source")
        );

        assertEquals(
                "partner_b",
                repository.findPayload(
                        "partner_b", "product", externalId
                ).orElseThrow().get("source")
        );

        assertEquals(1L, repository.count("olist", "product"));
        assertEquals(1L, repository.count("partner_b", "product"));
    }

    @Test
    void rejectsMissingSourceIdentity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.upsert(
                        " ",
                        "product",
                        newExternalId(),
                        Map.of("test", true)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.upsert(
                        "olist",
                        "product",
                        " ",
                        Map.of("test", true)
                )
        );
    }

    private UUID findRecordId(String externalId) {
        return jdbcTemplate.queryForObject("""
                SELECT id
                FROM external_source_records
                WHERE source_system = 'olist'
                  AND entity_type = 'product'
                  AND external_id = ?
                """,
                UUID.class,
                externalId
        );
    }


    private Instant findTimestamp(
            String externalId,
            String column
    ) {
        if (!column.equals("first_ingested_at")
                && !column.equals("last_ingested_at")) {
            throw new IllegalArgumentException(
                    "Unsupported timestamp column"
            );
        }

        String sql = """
            SELECT %s
            FROM external_source_records
            WHERE source_system = 'olist'
              AND entity_type = 'product'
              AND external_id = ?
            """.formatted(column);

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) ->
                        rs.getTimestamp(1).toInstant(),
                externalId
        );
    }

}
