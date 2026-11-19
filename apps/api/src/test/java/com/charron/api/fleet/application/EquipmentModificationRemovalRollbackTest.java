
package com.charron.api.fleet.application;

import com.charron.api.fleet.domain.EquipmentModification;
import com.charron.api.fleet.domain.EquipmentModificationEvent;
import com.charron.api.fleet.domain.EquipmentModificationEventType;
import com.charron.api.fleet.domain.EquipmentModificationType;
import com.charron.api.fleet.infrastructure.JdbcEquipmentModificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Transactional
class EquipmentModificationRemovalRollbackTest {

    @Autowired
    private EquipmentModificationRecordingService recordingService;

    @Autowired
    private EquipmentModificationRemovalService removalService;

    @Autowired
    private EquipmentModificationRepository repository;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private JdbcEquipmentModificationRepository jdbcRepository;

    @Test
    void rollsBackRemovedAtWhenEventInsertionFails() {
        UUID equipmentId = UUID.randomUUID();
        UUID modificationId = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO equipment (id, name, category, vin)
                VALUES (?, ?, ?, ?)
                """,
                equipmentId,
                "Removal Rollback Test",
                "OTHER",
                null
        );

        recordingService.record(new EquipmentModification(
                modificationId,
                equipmentId,
                null,
                EquipmentModificationType.INSTALLATION,
                "Installed auxiliary lighting",
                Instant.parse("2026-08-01T12:00:00Z"),
                Instant.parse("2026-09-01T12:00:00Z")
        ));

        // Fail only the REMOVED event insertion.
        doThrow(new IllegalStateException(
                "Simulated lifecycle event failure"
        )).when(jdbcRepository).insertEvent(
                argThat((EquipmentModificationEvent event) ->
                        event != null
                                && event.type() ==
                                EquipmentModificationEventType.REMOVED
                )
        );

        // Use a savepoint-backed nested transaction so we can
        // inspect the state after the failed operation.
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_NESTED
        );

        assertThrows(
                IllegalStateException.class,
                () -> transaction.executeWithoutResult(status ->
                        removalService.remove(
                                modificationId,
                                Instant.parse("2026-11-01T12:00:00Z")
                        )
                )
        );

        EquipmentModification modification =
                repository.findById(modificationId).orElseThrow();

        // The removed_at update must have rolled back.
        assertNull(modification.removedAt());

        // The original RECORDED event must remain.
        var events =
                repository.findEventsByModificationId(modificationId);

        assertEquals(1, events.size());

        assertEquals(
                EquipmentModificationEventType.RECORDED,
                events.get(0).type()
        );
    }
}
