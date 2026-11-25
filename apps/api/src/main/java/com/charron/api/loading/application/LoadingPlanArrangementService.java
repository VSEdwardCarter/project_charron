
package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import com.charron.api.loading.domain.LoadingPlanCargo;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoadingPlanArrangementService {

    private final LoadingPlanArrangementRepository arrangementRepository;
    private final LoadingPlanService loadingPlanService;
    private final LoadingPlanCargoRepository cargoRepository;

    private final JsonMapper objectMapper;
    private final LoadingMovementLock movementLock;



    public LoadingPlanArrangementService(
            LoadingPlanArrangementRepository arrangementRepository,
            LoadingPlanService loadingPlanService,
            LoadingPlanCargoRepository cargoRepository,

            JsonMapper objectMapper,
            LoadingMovementLock movementLock

    ) {
        this.arrangementRepository =
                Objects.requireNonNull(arrangementRepository);

        this.loadingPlanService =
                Objects.requireNonNull(loadingPlanService);

        this.cargoRepository =
                Objects.requireNonNull(cargoRepository);

        this.objectMapper =
                Objects.requireNonNull(objectMapper);

        this.movementLock =
                Objects.requireNonNull(movementLock);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LoadingPlanArrangement save(
            UUID loadingPlanId,
            String requestSnapshot,
            String previewSnapshot
    ) {
        if (loadingPlanId == null) {
            throw new InvalidLoadingPlanException(
                    "Loading plan ID is required"
            );
        }

        LoadingPlan plan = loadingPlanService.getById(
                loadingPlanId
        );

        if (!movementLock.acquire(plan.movementId())) {
            throw new LoadingPlanNotFoundException(
                    "Movement does not exist: " + plan.movementId()
            );
        }

        JsonNode request = parseObject(
                requestSnapshot,
                "Request snapshot"
        );

        JsonNode preview = parseObject(
                previewSnapshot,
                "Preview snapshot"
        );

        validateCargoSpace(
                request,
                plan.cargoSpaceId(),
                "Request"
        );

        validateCargoSpace(
                preview,
                plan.cargoSpaceId(),
                "Preview"
        );

        validateCargoAssignments(
                loadingPlanId,
                request
        );

        String status = requiredText(preview, "status");

        if (!List.of(
                "VALID",
                "INVALID",
                "INDETERMINATE",
                "NOT_PLACED"
        ).contains(status)) {
            throw new InvalidLoadingPlanException(
                    "Unsupported arrangement status: " + status
            );
        }

        if (!preview.path("placements").isArray()) {
            throw new InvalidLoadingPlanException(
                    "Preview must contain a placements array"
            );
        }

        Optional<LoadingPlanArrangement> existing =
                arrangementRepository.findByLoadingPlanId(
                        loadingPlanId
                );

        Instant now = Instant.now()
                .truncatedTo(ChronoUnit.MICROS);

        Instant savedAt = existing
                .map(LoadingPlanArrangement::savedAt)
                .orElse(now);

        LoadingPlanArrangement arrangement =
                new LoadingPlanArrangement(
                        loadingPlanId,
                        1,
                        requestSnapshot,
                        previewSnapshot,
                        status,
                        now,
                        savedAt,
                        now
                );

        arrangementRepository.save(arrangement);

        return arrangement;
    }

    @Transactional(readOnly = true)
    public Optional<LoadingPlanArrangement> findByLoadingPlanId(
            UUID loadingPlanId
    ) {
        if (loadingPlanId == null) {
            throw new InvalidLoadingPlanException(
                    "Loading plan ID is required"
            );
        }

        loadingPlanService.getById(loadingPlanId);

        return arrangementRepository.findByLoadingPlanId(
                loadingPlanId
        );
    }

    private void validateCargoSpace(
            JsonNode snapshot,
            UUID expectedCargoSpaceId,
            String label
    ) {
        String value = requiredText(
                snapshot,
                "cargoSpaceId"
        );

        if (!expectedCargoSpaceId.toString().equals(value)) {
            throw new InvalidLoadingPlanException(
                    label + " cargo space does not match loading plan"
            );
        }
    }

    private void validateCargoAssignments(
            UUID loadingPlanId,
            JsonNode request
    ) {
        JsonNode cargo = request.path("cargo");

        if (!cargo.isArray() || cargo.isEmpty()) {
            throw new InvalidLoadingPlanException(
                    "Request must contain assigned cargo"
            );
        }

        List<LoadingPlanCargo> assigned =
                cargoRepository.findByLoadingPlanId(
                        loadingPlanId
                );

        Map<UUID, Integer> expected = new HashMap<>();

        for (LoadingPlanCargo line : assigned) {
            expected.put(
                    line.cargoId(),
                    line.quantity()
            );
        }

        Map<UUID, Integer> actual = new HashMap<>();

        for (JsonNode item : cargo) {
            UUID cargoId;

            try {
                cargoId = UUID.fromString(
                        requiredText(item, "cargoId")
                );
            } catch (IllegalArgumentException exception) {
                throw new InvalidLoadingPlanException(
                        "Request contains an invalid cargo ID"
                );
            }

            JsonNode quantityNode = item.path("quantity");

            if (!quantityNode.isIntegralNumber()
                    || !quantityNode.canConvertToInt()
                    || quantityNode.intValue() <= 0) {
                throw new InvalidLoadingPlanException(
                        "Cargo quantity must be a positive integer"
                );
            }

            if (actual.putIfAbsent(
                    cargoId,
                    quantityNode.intValue()
            ) != null) {
                throw new InvalidLoadingPlanException(
                        "Request contains duplicate cargo IDs"
                );
            }
        }

        if (expected.isEmpty() || !expected.equals(actual)) {
            throw new LoadingPlanConflictException(
                    "Arrangement cargo does not match the "
                            + "current loading plan assignments"
            );
        }
    }

    private JsonNode parseObject(
            String json,
            String label
    ) {
        if (json == null || json.isBlank()) {
            throw new InvalidLoadingPlanException(
                    label + " is required"
            );
        }

        try {
            JsonNode node = objectMapper.readTree(json);

            if (node == null || !node.isObject()) {
                throw new InvalidLoadingPlanException(
                        label + " must be a JSON object"
                );
            }

            return node;

        } catch (JacksonException exception) {

            throw new InvalidLoadingPlanException(
                    label + " contains invalid JSON"
            );
        }
    }

    private String requiredText(
            JsonNode node,
            String field
    ) {
        JsonNode value = node.path(field);

        if (!value.isTextual()
                || value.asText().isBlank()) {
            throw new InvalidLoadingPlanException(
                    "Missing or invalid field: " + field
            );
        }

        return value.asText();
    }
}
