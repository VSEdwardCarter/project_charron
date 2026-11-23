
package com.charron.api.loading.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoadingPlanArrangementTest {

    private static final String REQUEST =
            """
            {"cargoSpaceId":"test-space","cargo":[]}
            """;

    private static final String PREVIEW =
            """
            {"status":"VALID","reason":"Valid","placements":[]}
            """;

    @Test
    void createsArrangement() {
        UUID planId = UUID.randomUUID();

        LoadingPlanArrangement arrangement =
                LoadingPlanArrangement.create(
                        planId,
                        REQUEST,
                        PREVIEW,
                        "VALID"
                );

        assertEquals(planId, arrangement.loadingPlanId());
        assertEquals(1, arrangement.formatVersion());
        assertEquals("VALID", arrangement.status());
        assertEquals(REQUEST, arrangement.requestSnapshot());
        assertEquals(PREVIEW, arrangement.previewSnapshot());
        assertEquals(
                arrangement.savedAt(),
                arrangement.updatedAt()
        );
    }

    @Test
    void rejectsMissingPlanId() {
        assertThrows(
                NullPointerException.class,
                () -> LoadingPlanArrangement.create(
                        null,
                        REQUEST,
                        PREVIEW,
                        "VALID"
                )
        );
    }

    @Test
    void rejectsInvalidFormatVersion() {
        Instant now = Instant.now();

        assertThrows(
                IllegalArgumentException.class,
                () -> new LoadingPlanArrangement(
                        UUID.randomUUID(),
                        0,
                        REQUEST,
                        PREVIEW,
                        "VALID",
                        now,
                        now,
                        now
                )
        );
    }

    @Test
    void rejectsUnsupportedStatus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LoadingPlanArrangement.create(
                        UUID.randomUUID(),
                        REQUEST,
                        PREVIEW,
                        "UNKNOWN"
                )
        );
    }

    @Test
    void rejectsBlankSnapshots() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LoadingPlanArrangement.create(
                        UUID.randomUUID(),
                        " ",
                        PREVIEW,
                        "VALID"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> LoadingPlanArrangement.create(
                        UUID.randomUUID(),
                        REQUEST,
                        "",
                        "VALID"
                )
        );
    }

    @Test
    void rejectsInvalidTimestamps() {
        Instant saved = Instant.parse("2026-12-01T12:00:00Z");
        Instant updated = Instant.parse("2026-12-01T11:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> new LoadingPlanArrangement(
                        UUID.randomUUID(),
                        1,
                        REQUEST,
                        PREVIEW,
                        "VALID",
                        saved,
                        saved,
                        updated
                )
        );
    }
}
