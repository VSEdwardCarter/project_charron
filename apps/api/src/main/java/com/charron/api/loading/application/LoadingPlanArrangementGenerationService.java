
package com.charron.api.loading.application;

import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import com.charron.api.loading.domain.LoadingPlanCargo;
import com.charron.api.loading.web.LoadingPreviewRequest;
import com.charron.api.loading.web.LoadingPreviewResponse;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class LoadingPlanArrangementGenerationService {

    private final LoadingPlanService loadingPlanService;
    private final LoadingPlanCargoRepository cargoRepository;
    private final LoadingCargoCatalogLookup catalogLookup;
    private final LoadingPreviewService previewService;
    private final LoadingPlanArrangementService arrangementService;

    private final JsonMapper objectMapper;


    public LoadingPlanArrangementGenerationService(
            LoadingPlanService loadingPlanService,
            LoadingPlanCargoRepository cargoRepository,
            LoadingCargoCatalogLookup catalogLookup,
            LoadingPreviewService previewService,
            LoadingPlanArrangementService arrangementService,

            JsonMapper objectMapper

    ) {
        this.loadingPlanService =
                Objects.requireNonNull(loadingPlanService);
        this.cargoRepository =
                Objects.requireNonNull(cargoRepository);
        this.catalogLookup =
                Objects.requireNonNull(catalogLookup);
        this.previewService =
                Objects.requireNonNull(previewService);
        this.arrangementService =
                Objects.requireNonNull(arrangementService);
        this.objectMapper =
                Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public LoadingPlanArrangement generateAndSave(
            UUID loadingPlanId
    ) {
        if (loadingPlanId == null) {
            throw new InvalidLoadingPlanException(
                    "Loading plan ID is required"
            );
        }

        LoadingPlan plan =
                loadingPlanService.getById(loadingPlanId);

        List<LoadingPlanCargo> assignedCargo =
                cargoRepository.findByLoadingPlanId(loadingPlanId);

        if (assignedCargo.isEmpty()) {
            throw new InvalidLoadingPlanException(
                    "Add cargo to the loading plan before generating an arrangement"
            );
        }

        List<LoadingPreviewRequest.CargoItem> requestCargo =
                new ArrayList<>();

        for (LoadingPlanCargo line : assignedCargo) {
            LoadingCargoCatalogLookup.CargoDetails cargo =
                    catalogLookup.findById(line.cargoId())
                            .orElseThrow(() ->
                                    new LoadingPlanNotFoundException(
                                            "Catalog cargo does not exist: "
                                                    + line.cargoId()
                                    )
                            );

            if (cargo.lengthMm() == null
                    || cargo.widthMm() == null
                    || cargo.heightMm() == null
                    || cargo.lengthMm() <= 0
                    || cargo.widthMm() <= 0
                    || cargo.heightMm() <= 0) {
                throw new InvalidLoadingPlanException(
                        "Cargo requires complete positive dimensions: "
                                + cargo.name()
                );
            }

            requestCargo.add(
                    new LoadingPreviewRequest.CargoItem(
                            cargo.id(),
                            cargo.name(),
                            line.quantity(),
                            cargo.lengthMm(),
                            cargo.widthMm(),
                            cargo.heightMm(),
                            cargo.weightGrams()
                    )
            );
        }

        LoadingPreviewRequest request =
                new LoadingPreviewRequest(
                        plan.cargoSpaceId(),
                        List.copyOf(requestCargo)
                );

        // Reuse CHARRON's existing placement generator
        // and validation engine.
        LoadingPreviewResponse preview =
                previewService.preview(request);

        // The backend, not the browser, generated this result.
        String requestJson = serialize(request);
        String previewJson = serialize(preview);

        return arrangementService.save(
                loadingPlanId,
                requestJson,
                previewJson
        );
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);

        } catch (JacksonException exception) {

            throw new IllegalStateException(
                    "Unable to serialize loading arrangement",
                    exception
            );
        }
    }
}
