package com.smokefree.program.web.dto.plan;

import java.util.UUID;

public record PlanRecommendationRes(
        String severity,
        UUID planTemplateId,
        String planCode,
        String planName,
        Integer durationDays,
        String rationale
) {}
