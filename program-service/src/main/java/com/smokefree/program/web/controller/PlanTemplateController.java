package com.smokefree.program.web.controller;


import com.smokefree.program.domain.service.template.PlanTemplateService;
import com.smokefree.program.web.dto.plan.PlanRecommendationRes;
import com.smokefree.program.web.dto.plan.PlanTemplateDetailRes;
import com.smokefree.program.web.dto.plan.PlanTemplateSummaryRes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plan-templates")
public class PlanTemplateController {

    private final PlanTemplateService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<PlanTemplateSummaryRes> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PlanTemplateDetailRes detail(@PathVariable UUID id) {
        return service.getDetail(id);
    }

    @GetMapping("/recommendation")
    @PreAuthorize("isAuthenticated()")
    public PlanRecommendationRes recommend(@RequestParam String severity) {
        return service.recommendBySeverity(severity);
    }
}
