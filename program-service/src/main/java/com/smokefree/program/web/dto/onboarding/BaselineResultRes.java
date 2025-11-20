// src/main/java/com/smokefree/program/web/dto/onboarding/BaselineResultRes.java
package com.smokefree.program.web.dto.onboarding;

import com.smokefree.program.domain.model.SeverityLevel;

import java.util.List;
import java.util.UUID;

public record BaselineResultRes(
        int totalScore,
        SeverityLevel severity,
        UUID recommendedTemplateId,
        String recommendedTemplateCode,
        List<PlanOption> options
) {}
