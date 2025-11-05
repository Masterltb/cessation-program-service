package com.smokefree.program.web.dto.plan;

import java.util.List;
import java.util.UUID;

public record PlanTemplateDetailRes(
        UUID id,
        String code,
        String name,
        String description,
        Integer durationDays,
        List<StepRes> steps
) {
    public record StepRes(int stepNo, String title, String content, Integer dayOffset, String type) {}
}
