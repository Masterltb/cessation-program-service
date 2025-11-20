// src/main/java/com/smokefree/program/web/controller/OnboardingController.java
package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.onboarding.OnboardingFlowService;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.onboarding.BaselineResultRes;
import com.smokefree.program.web.dto.quiz.QuizAnswerReq;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingFlowService onboardingFlowService;

    @PostMapping("/baseline")
    public BaselineResultRes submitBaseline(@RequestBody QuizAnswerReq req,
                                            @RequestHeader(name = "X-User-Tier", required = false)
                                            String userTier) {

        UUID userId = SecurityUtil.requireUserId();
        return onboardingFlowService.submitBaselineAndRecommend(userId, req, userTier);
    }
}
