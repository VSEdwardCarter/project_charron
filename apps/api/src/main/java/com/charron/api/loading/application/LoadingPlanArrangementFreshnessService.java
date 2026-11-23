
package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import com.charron.api.loading.domain.LoadingPlanCargo;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class LoadingPlanArrangementFreshnessService {

    private final LoadingPlanService loadingPlanService;
    private final LoadingPlanCargoRepository cargoRepository;
    private final LoadingCargoCatalogLookup catalogLookup;

    private final JsonMapper objectMapper;


    public LoadingPlanArrangementFreshnessService(
            LoadingPlanService loadingPlanService,
            LoadingPlanCargoRepository cargoRepository,
            LoadingCargoCatalogLookup catalogLookup,
            JsonMapper objectMapper


    ) {
        this.loadingPlanService =
                Objects.requireNonNull(loadingPlanService);
        this.cargoRepository =
                Objects.requireNonNull(cargoRepository);
        this.catalogLookup =
                Objects.requireNonNull(catalogLookup);
        this.objectMapper =
                Objects.requireNonNull(objectMapper);
    }

    @Transactional(readOnly = true)
    public boolean isCurrent(
            LoadingPlanArrangement arrangement
    ) {
        Objects.requireNonNull(
                arrangement,
                "Arrangement is required"
        );

        LoadingPlan plan = loadingPlanService.getById(
                arrangement.loadingPlanId()
        );

        JsonNode request;

        try {
            request = objectMapper.readTree(
                    arrangement.requestSnapshot()
            );
        } catch (Exception exception) {
            return false;
        }

        if (request == null || !request.isObject()) {
            return false;
        }

        if (!plan.cargoSpaceId().toString().equals(
                request.path("cargoSpaceId").asText()
        )) {
            return false;
        }

        JsonNode savedCargo = request.path("cargo");

        if (!savedCargo.isArray() || savedCargo.isEmpty()) {
            return false;
        }

        List<LoadingPlanCargo> currentLines =
                cargoRepository.findByLoadingPlanId(
                        arrangement.loadingPlanId()
                );

        if (savedCargo.size() != currentLines.size()) {
            return false;
        }

        Map<UUID, LoadingPlanCargo> currentByCargoId =
                new HashMap<>();

        for (LoadingPlanCargo line : currentLines) {
            if (currentByCargoId.putIfAbsent(
                    line.cargoId(),
                    line
            ) != null) {
                return false;
            }
        }

        for (JsonNode saved : savedCargo) {
            if (!saved.isObject()) {
                return false;
            }

            UUID cargoId;

            try {
                cargoId = UUID.fromString(
                        saved.path("cargoId").asText()
                );
            } catch (IllegalArgumentException exception) {
                return false;
            }

            LoadingPlanCargo current =
                    currentByCargoId.remove(cargoId);

            if (current == null) {
                return false;
            }

            JsonNode savedQuantity = saved.path("quantity");

            if (!savedQuantity.canConvertToInt()
                    || !savedQuantity.isIntegralNumber()
                    || savedQuantity.intValue() != current.quantity()) {
                return false;
            }

            var catalogItem = catalogLookup
                    .findById(cargoId);

            if (catalogItem.isEmpty()) {
                return false;
            }

            var cargo = catalogItem.orElseThrow();

            if (!Objects.equals(
                    saved.path("name").asText(null),
                    cargo.name()
            )) {
                return false;
            }

            if (!matchesLong(
                    saved.path("lengthMm"),
                    cargo.lengthMm()
            )) {
                return false;
            }

            if (!matchesLong(
                    saved.path("widthMm"),
                    cargo.widthMm()
            )) {
                return false;
            }

            if (!matchesLong(
                    saved.path("heightMm"),
                    cargo.heightMm()
            )) {
                return false;
            }

            if (!matchesLong(
                    saved.path("weightGrams"),
                    cargo.weightGrams()
            )) {
                return false;
            }
        }

        return currentByCargoId.isEmpty();
    }

    private boolean matchesLong(
            JsonNode node,
            Long expected
    ) {
        if (expected == null) {
            return node.isMissingNode() || node.isNull();
        }

        return node.isIntegralNumber()
                && node.canConvertToLong()
                && node.longValue() == expected;
    }
}
