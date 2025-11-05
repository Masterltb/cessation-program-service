package com.smokefree.program.web.dto.subscription;

import java.time.Instant;

public record SubscriptionStatusRes(
        String tier,          // BASIC|PREMIUM|VIP
        Instant entExpiresAt, // null nếu BASIC/trial
        String[] entitlements // ví dụ: ["FORUM","DM_COACH"]
) {}

