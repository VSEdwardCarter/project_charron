
package com.charron.api.integration.infrastructure;

import com.charron.api.integration.application.ExternalSourceIngestionService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class OlistProductCsvAdapter {

    private final ExternalSourceIngestionService ingestionService;

    public OlistProductCsvAdapter(
            ExternalSourceIngestionService ingestionService
    ) {
        this.ingestionService = Objects.requireNonNull(ingestionService);
    }

    public int ingest(Path csvPath, int limit) throws IOException {
        Objects.requireNonNull(csvPath, "CSV path is required");

        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "Ingestion limit must be positive"
            );
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        int ingested = 0;

        try (CSVParser parser = CSVParser.parse(
                csvPath.toFile(),
                StandardCharsets.UTF_8,
                format
        )) {
            for (CSVRecord record : parser) {
                if (ingested >= limit) {
                    break;
                }

                Map<String, Object> payload =
                        new LinkedHashMap<>(record.toMap());

                String productId = (String) payload.get("product_id");

                if (productId == null || productId.isBlank()) {
                    throw new IllegalArgumentException(
                            "Missing product_id at CSV record "
                                    + record.getRecordNumber()
                    );
                }

                ingestionService.ingest(
                        "olist",
                        "product",
                        productId,
                        payload
                );

                ingested++;
            }
        }

        return ingested;
    }
}
