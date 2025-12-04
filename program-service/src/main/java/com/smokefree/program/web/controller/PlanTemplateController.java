// src/main/java/com/smokefree/program/web/controller/PlanTemplateController.java
package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.template.PlanTemplateService;
import com.smokefree.program.web.dto.plan.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plan-templates")
public class PlanTemplateController {

    private final PlanTemplateService service;

    @GetMapping
    public List<PlanTemplateSummaryRes> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public PlanTemplateDetailRes detail(@PathVariable UUID id) {
        return service.getDetail(id);
    }

    @GetMapping("/by-code/{code}/days")
    public PlanDaysRes daysByCode(@PathVariable String code,
                                  @RequestParam(defaultValue = "false") boolean expand,
                                  @RequestParam(defaultValue = "vi") String lang) {
        return service.getDaysByCode(code, expand, lang);
    }

    @GetMapping("/{id}/days")
    public PlanDaysRes days(@PathVariable UUID id,
                            @RequestParam(defaultValue = "false") boolean expand,
                            @RequestParam(defaultValue = "vi") String lang) {
        return service.getDays(id, expand, lang);
    }

    @GetMapping("/recommendation")
    public PlanRecommendationRes recommend(@RequestParam String severity) {
        return service.recommendBySeverity(severity);
    }
}