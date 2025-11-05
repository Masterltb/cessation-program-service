package com.smokefree.program.domain.service;

import com.smokefree.program.web.dto.subscription.SubscriptionStatusRes;
import com.smokefree.program.web.dto.subscription.UpgradeReq;

import java.util.UUID;

public interface SubscriptionService {
    SubscriptionStatusRes getStatus(UUID userId);
    SubscriptionStatusRes upgrade(UUID userId, UpgradeReq req);
}
