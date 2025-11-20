package com.smokefree.program.web.dto.onboarding;

import java.util.UUID;

public record PlanOption(
        UUID id,
        String code,
        String name,
        int totalDays,
        boolean recommended
) {}