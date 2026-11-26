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
class OlistSellerCsvAdapterTest {

    @Autowired
    private OlistSellerCsvAdapter adapter;

    @Autowired
    private ExternalSourceRecordRepository repository;

    private Path sellerCsvPath() {

        Path csvPath = Path.of(
                System.getProperty("user.home"),
                "Desktop",
                "Repos",
                "charron-data",
                "olist",
                "raw",
                "olist_sellers_dataset.csv"
        );

        assertTrue(
                Files.isRegularFile(csvPath),
                "Olist seller CSV must exist at " + csvPath
        );

        return csvPath;
    }

    @Test
    void ingestsFiveRealOlistSellers() throws Exception {

        Path csvPath = sellerCsvPath();

        assertEquals(5, adapter.ingest(csvPath, 5));

        Map<String, Object> seller = repository.findPayload(
                "olist",
                "seller",
                "3442f8959a84dea7ee197c632cb2df15"
        ).orElseThrow();

        assertEquals("campinas", seller.get("seller_city"));
        assertEquals("SP", seller.get("seller_state"));
        assertEquals("13023", seller.get("seller_zip_code_prefix"));
        assertEquals(4, seller.size());

        assertTrue(
                repository.count("olist", "seller") >= 5
        );
    }

    @Test
    void replayingSameCsvDoesNotDuplicateSellers()
            throws Exception {

        Path csvPath = sellerCsvPath();

        long initialCount =
                repository.count("olist", "seller");

        assertEquals(5, adapter.ingest(csvPath, 5));

        long countAfterFirstIngestion =
                repository.count("olist", "seller");

        assertTrue(countAfterFirstIngestion >= 5);

        assertTrue(
                countAfterFirstIngestion >= initialCount
        );

        assertTrue(
                countAfterFirstIngestion <= initialCount + 5
        );

        assertEquals(5, adapter.ingest(csvPath, 5));

        assertEquals(
                countAfterFirstIngestion,
                repository.count("olist", "seller"),
                "Replaying the same CSV must not create duplicates"
        );
    }
}