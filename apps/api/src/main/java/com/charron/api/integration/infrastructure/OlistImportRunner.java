package com.charron.api.integration.infrastructure;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(
        name = "charron.ingestion.olist.enabled",
        havingValue = "true"
)
public class OlistImportRunner implements ApplicationRunner {

    private final OlistProductCsvAdapter adapter;

    public OlistImportRunner(OlistProductCsvAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String path = requiredOption(
                args, "charron.ingestion.olist.path"
        );

        String limitValue = requiredOption(
                args, "charron.ingestion.olist.limit"
        );

        int limit = Integer.parseInt(limitValue);

        int imported = adapter.ingest(Path.of(path), limit);

        System.out.printf(
                "CHARRON Olist import completed: %d records processed%n",
                imported
        );
    }

    private static String requiredOption(
            ApplicationArguments args,
            String name
    ) {
        var values = args.getOptionValues(name);

        if (values == null || values.size() != 1
                || values.getFirst().isBlank()) {
            throw new IllegalArgumentException(
                    "Required option missing: --" + name
            );
        }

        return values.getFirst();
    }
}
