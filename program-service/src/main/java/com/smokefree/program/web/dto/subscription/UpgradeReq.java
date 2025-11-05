package com.smokefree.program.web.dto.subscription;

import jakarta.validation.constraints.NotNull;

public record UpgradeReq(
        @NotNull String targetTier // PREMIUM | VIP
) {}
