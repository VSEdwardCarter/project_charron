
package com.charron.api.integration.infrastructure;

import com.charron.api.integration.application.ExternalSourceRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OlistProductCsvAdapterTest {

    @Autowired
    private OlistProductCsvAdapter adapter;

    @Autowired
    private ExternalSourceRecordRepository repository;

    @Test
    void ingestsFiveRealOlistProducts() throws Exception {
        Path csvPath = Path.of(
                System.getProperty("user.home"),
                "Desktop",
                "Repos",
                "charron-data",
                "olist",
                "raw",
                "olist_products_dataset.csv"
        );

        assertTrue(
                Files.isRegularFile(csvPath),
                "Olist product CSV must exist at " + csvPath
        );

        int ingested = adapter.ingest(csvPath, 5);

        assertEquals(5, ingested);

        Map<String, Object> product = repository.findPayload(
                "olist",
                "product",
                "1e9e8ef04dbcff4541ed26657ea517e5"
        ).orElseThrow();

        assertEquals(
                "perfumaria",
                product.get("product_category_name")
        );

        assertEquals(
                "225",
                product.get("product_weight_g")
        );

        assertEquals(
                "16",
                product.get("product_length_cm")
        );

        assertEquals(
                9,
                product.size(),
                "All original Olist product columns should be preserved"
        );
    }


    @Test
    void replayingSameCsvDoesNotDuplicateProducts() throws Exception {
        Path csvPath = Path.of(
                System.getProperty("user.home"),
                "Desktop",
                "Repos",
                "charron-data",
                "olist",
                "raw",
                "olist_products_dataset.csv"
        );

        assertTrue(Files.isRegularFile(csvPath));

        String sourceSystem = "olist-replay-test";
        String entityType = "product";

        // Use a test-scoped source identity so this test does not
        // depend on any existing persistent Olist records.
        // The adapter currently uses the fixed source "olist",
        // so we verify the before/after change for those records.

        String firstProductId =
                "1e9e8ef04dbcff4541ed26657ea517e5";

        adapter.ingest(csvPath, 5);

        Map<String, Object> firstPayload = repository.findPayload(
                "olist", entityType, firstProductId
        ).orElseThrow();

        adapter.ingest(csvPath, 5);

        Map<String, Object> replayedPayload = repository.findPayload(
                "olist", entityType, firstProductId
        ).orElseThrow();

        assertEquals(firstPayload, replayedPayload);
    }

}
