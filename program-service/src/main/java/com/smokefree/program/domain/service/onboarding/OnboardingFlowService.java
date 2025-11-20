// src/main/java/com/smokefree/program/domain/service/onboarding/OnboardingFlowService.java
package com.smokefree.program.domain.service.onboarding;

import com.smokefree.program.web.dto.onboarding.BaselineResultRes;
import com.smokefree.program.web.dto.quiz.QuizAnswerReq;

import java.util.UUID;

public interface OnboardingFlowService {

    /**
     * User mới submit baseline quiz → trả severity + các gói lộ trình với một gói được recommend.
     */
    BaselineResultRes submitBaselineAndRecommend(UUID userId,
                                                 QuizAnswerReq req,
                                                 String userTier);

}
