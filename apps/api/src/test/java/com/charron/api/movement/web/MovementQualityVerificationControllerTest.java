package com.charron.api.movement.web;

import com.charron.api.movement.application.ManifestFreshnessConflictException;
import com.charron.api.movement.application.ManifestLifecycleConflictException;
import com.charron.api.movement.application.ManifestNotFoundException;
import com.charron.api.movement.application.MovementQualityVerificationService;
import com.charron.api.movement.domain.ManifestFreshnessAssessment;
import com.charron.api.movement.domain.ManifestFreshnessStatus;
import com.charron.api.movement.domain.MovementQualityVerification;
import com.charron.api.movement.application.MovementQualityVerificationService.VerificationStatus;
import org.junit.jupiter.params.provider.EnumSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MovementQualityVerificationControllerTest {

    private MovementQualityVerificationService service;

    private MockMvc mockMvc;

    private final UUID movementId = UUID.randomUUID();

    private final UUID manifestId = UUID.randomUUID();

    private final UUID reviewerId = UUID.randomUUID();

    private final UUID verificationId = UUID.randomUUID();

    private final Instant verifiedAt =
            Instant.parse("2026-11-25T20:00:00Z");

    private String endpoint;

    @BeforeEach
    void setUp() {

        service = mock(
                MovementQualityVerificationService.class
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new MovementQualityVerificationController(
                                service
                        )
                )
                .build();

        endpoint = "/api/movements/"
                + movementId
                + "/manifests/"
                + manifestId
                + "/quality-verifications";
    }

    @Test
    void returnsVerificationHistory() throws Exception {

        when(service.getHistory(movementId, manifestId))
                .thenReturn(List.of(completedVerification()));

        mockMvc.perform(get(endpoint))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.length()").value(1))

                .andExpect(jsonPath("$[0].id")
                        .value(verificationId.toString()))

                .andExpect(jsonPath("$[0].manifestId")
                        .value(manifestId.toString()))

                .andExpect(jsonPath("$[0].reviewerId")
                        .value(reviewerId.toString()))

                .andExpect(jsonPath(
                        "$[0].cargoReconciliationVerified"
                ).value(true))

                .andExpect(jsonPath("$[0].remarks")
                        .value("Inspection completed"));

        verify(service).getHistory(movementId, manifestId);
    }

    @Test
    void returnsEmptyVerificationHistory() throws Exception {

        when(service.getHistory(movementId, manifestId))
                .thenReturn(List.of());

        mockMvc.perform(get(endpoint))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest
    @EnumSource(VerificationStatus.class)
    void returnsVerificationStatus(
            VerificationStatus verificationStatus
    ) throws Exception {

        when(service.getVerificationStatus(
                movementId,
                manifestId
        )).thenReturn(verificationStatus);

        mockMvc.perform(
                        get(endpoint + "/status")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value(verificationStatus.name()));

        verify(service).getVerificationStatus(
                movementId,
                manifestId
        );
    }

    @Test
    void returnsNotFoundForUnknownManifestStatus()
            throws Exception {

        when(service.getVerificationStatus(
                movementId,
                manifestId
        )).thenThrow(
                new ManifestNotFoundException(manifestId)
        );

        mockMvc.perform(
                        get(endpoint + "/status")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("MANIFEST_NOT_FOUND"));

        verify(service).getVerificationStatus(
                movementId,
                manifestId
        );
    }

    @Test
    void createsCompletedVerification() throws Exception {

        when(service.complete(
                movementId,
                manifestId,
                reviewerId,
                true, true, true,
                true, true, true,
                "Inspection completed"
        )).thenReturn(completedVerification());

        mockMvc.perform(
                        post(endpoint)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest())
                )

                .andExpect(status().isCreated())

                .andExpect(jsonPath("$.id")
                        .value(verificationId.toString()))

                .andExpect(jsonPath("$.manifestId")
                        .value(manifestId.toString()))

                .andExpect(jsonPath("$.reviewerId")
                        .value(reviewerId.toString()))

                .andExpect(jsonPath(
                        "$.equipmentAssignmentVerified"
                ).value(true))

                .andExpect(jsonPath(
                        "$.equipmentInspectionCompleted"
                ).value(true))

                .andExpect(jsonPath(
                        "$.cargoReconciliationVerified"
                ).value(true))

                .andExpect(jsonPath(
                        "$.loadingArrangementVerified"
                ).value(true))

                .andExpect(jsonPath(
                        "$.cargoSecurementVerified"
                ).value(true))

                .andExpect(jsonPath(
                        "$.documentationVerified"
                ).value(true));

        verify(service).complete(
                movementId,
                manifestId,
                reviewerId,
                true, true, true,
                true, true, true,
                "Inspection completed"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "equipmentAssignmentVerified",
            "equipmentInspectionCompleted",
            "cargoReconciliationVerified",
            "loadingArrangementVerified",
            "cargoSecurementVerified",
            "documentationVerified"
    })
    void rejectsIncompleteVerification(
            String incompleteField
    ) throws Exception {

        String request = validRequest().replace(
                "\"" + incompleteField + "\": true",
                "\"" + incompleteField + "\": false"
        );

        mockMvc.perform(
                        post(endpoint)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingReviewerIdentity() throws Exception {

        String request = validRequest().replace(
                "\"" + reviewerId + "\"",
                "null"
        );

        mockMvc.perform(
                        post(endpoint)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void rejectsMalformedRequest() throws Exception {

        mockMvc.perform(
                        post(endpoint)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{invalid-json")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void returnsNotFoundForUnknownManifest() throws Exception {

        when(service.getHistory(movementId, manifestId))
                .thenThrow(
                        new ManifestNotFoundException(manifestId)
                );

        mockMvc.perform(get(endpoint))

                .andExpect(status().isNotFound())

                .andExpect(jsonPath("$.code")
                        .value("MANIFEST_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenCompletingUnknownManifest()
            throws Exception {

        when(service.complete(
                movementId,
                manifestId,
                reviewerId,
                true, true, true,
                true, true, true,
                "Inspection completed"
        )).thenThrow(
                new ManifestNotFoundException(manifestId)
        );

        mockMvc.perform(
                        post(endpoint)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest())
                )

                .andExpect(status().isNotFound())

                .andExpect(jsonPath("$.code")
                        .value("MANIFEST_NOT_FOUND"));
    }

    @Test
    void returnsConflictForInvalidManifestLifecycle()
            throws Exception {

        when(service.complete(
                movementId,
                manifestId,
                reviewerId,
                true, true, true,
                true, true, true,
                "Inspection completed"
        )).thenThrow(
                new ManifestLifecycleConflictException(
                        manifestId,
                        "Quality verification requires an issued manifest"
                )
        );

        mockMvc.perform(
                        post(endpoint)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest())
                )

                .andExpect(status().isConflict())

                .andExpect(jsonPath("$.code")
                        .value("MANIFEST_LIFECYCLE_CONFLICT"));
    }

    @Test
    void returnsConflictForStaleManifest() throws Exception {

        ManifestFreshnessAssessment freshness =
                mock(ManifestFreshnessAssessment.class);

        when(freshness.manifestId())
                .thenReturn(manifestId);

        when(freshness.status())
                .thenReturn(ManifestFreshnessStatus.STALE);

        when(freshness.changes())
                .thenReturn(Set.of());

        when(freshness.reason())
                .thenReturn("Operational data has changed");

        ManifestFreshnessConflictException exception =
                new ManifestFreshnessConflictException(freshness);

        when(service.complete(
                movementId,
                manifestId,
                reviewerId,
                true, true, true,
                true, true, true,
                "Inspection completed"
        )).thenThrow(exception);

        mockMvc.perform(
                        post(endpoint)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("MANIFEST_FRESHNESS_CONFLICT"));
    }

    private MovementQualityVerification completedVerification() {

        return new MovementQualityVerification(
                verificationId,
                manifestId,
                reviewerId,
                true, true, true,
                true, true, true,
                verifiedAt,
                "Inspection completed"
        );
    }

    private String validRequest() {

        return """
                {
                    "reviewerId": "%s",
                    "equipmentAssignmentVerified": true,
                    "equipmentInspectionCompleted": true,
                    "cargoReconciliationVerified": true,
                    "loadingArrangementVerified": true,
                    "cargoSecurementVerified": true,
                    "documentationVerified": true,
                    "remarks": "Inspection completed"
                }
                """.formatted(reviewerId);
    }
}