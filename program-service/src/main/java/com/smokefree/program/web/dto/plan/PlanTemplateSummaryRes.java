package com.smokefree.program.web.dto.plan;

import java.util.UUID;

public record PlanTemplateSummaryRes(
        UUID id,
        String code,
        String name,
        String description,
        Integer durationDays
) {}

