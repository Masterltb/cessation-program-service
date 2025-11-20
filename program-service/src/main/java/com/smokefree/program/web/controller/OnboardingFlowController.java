//// src/main/java/com/smokefree/program/web/controller/onboarding/OnboardingFlowController.java
//package com.smokefree.program.web.controller.onboarding;
//
//import com.smokefree.program.domain.service.onboarding.OnboardingFlowService;
//import com.smokefree.program.util.EntitlementUtil;
//import com.smokefree.program.util.SecurityUtil;
//import com.smokefree.program.web.dto.onboarding.BaselineResultRes;
//import com.smokefree.program.web.dto.quiz.QuizAnswerReq;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/onboarding")
//@RequiredArgsConstructor
//public class OnboardingFlowController {
//
//    private final OnboardingFlowService onboardingFlowService;
//
//    @PostMapping("/baseline")
//    @PreAuthorize("isAuthenticated()")
//    public BaselineResultRes baseline(@RequestBody @Valid QuizAnswerReq req) {
//        UUID userId = SecurityUtil.requireUserId();
//        String tier = EntitlementUtil.currentTier();
//        return onboardingFlowService.submitBaselineAndRecommend(userId, req, tier);
//    }
//}
