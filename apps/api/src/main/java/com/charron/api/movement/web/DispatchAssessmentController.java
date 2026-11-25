
package com.charron.api.movement.web;

import com.charron.api.movement.application.DispatchAssessment;
import com.charron.api.movement.application.DispatchAssessmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/movements")
public class DispatchAssessmentController {

    private final DispatchAssessmentService service;

    public DispatchAssessmentController(
            DispatchAssessmentService service
    ) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping("/{id}/dispatch-assessment")
    public DispatchAssessment assess(
            @PathVariable UUID id
    ) {
        return service.assess(id);
    }
}
