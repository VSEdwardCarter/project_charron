
package com.charron.api.loading.api;

import com.charron.api.loading.application.LoadingPlanArrangementFreshnessService;
import com.charron.api.loading.application.LoadingPlanArrangementService;
import com.charron.api.loading.application.LoadingPlanConflictException;
import com.charron.api.loading.application.LoadingPlanNotFoundException;
import com.charron.api.loading.domain.LoadingPlanArrangement;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/loading-plans/{planId}/arrangement/current")
public class LoadingPlanCurrentArrangementController {

    private final LoadingPlanArrangementService arrangementService;
    private final LoadingPlanArrangementFreshnessService freshnessService;

    public LoadingPlanCurrentArrangementController(
            LoadingPlanArrangementService arrangementService,
            LoadingPlanArrangementFreshnessService freshnessService
    ) {
        this.arrangementService =
                Objects.requireNonNull(arrangementService);

        this.freshnessService =
                Objects.requireNonNull(freshnessService);
    }

    @GetMapping
    public LoadingPlanArrangement getCurrentArrangement(
            @PathVariable UUID planId
    ) {
        LoadingPlanArrangement arrangement =
                arrangementService.findByLoadingPlanId(planId)
                        .orElseThrow(() ->
                                new LoadingPlanNotFoundException(
                                        "No saved arrangement exists for loading plan: "
                                                + planId
                                )
                        );

        if (!freshnessService.isCurrent(arrangement)) {
            throw new LoadingPlanConflictException(
                    "The saved arrangement is outdated. "
                            + "Regenerate the loading arrangement "
                            + "using the current cargo assignments."
            );
        }

        return arrangement;
    }
}
