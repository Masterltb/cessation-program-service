package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.SubscriptionService;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.subscription.SubscriptionStatusRes;
import com.smokefree.program.web.dto.subscription.UpgradeReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService service;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public SubscriptionStatusRes me() {
        return service.getStatus(SecurityUtil.currentUserId());
    }

    @PostMapping("/upgrade")
    @PreAuthorize("isAuthenticated()")
    public SubscriptionStatusRes upgrade(@RequestBody @Valid UpgradeReq req) {
        return service.upgrade(SecurityUtil.currentUserId(), req);
    }
}
