// src/main/java/com/smokefree/program/domain/service/onboarding/OnboardingFlowServiceImpl.java
package com.smokefree.program.domain.service.onboarding;

import com.smokefree.program.domain.model.PlanTemplate;
import com.smokefree.program.domain.model.SeverityLevel;
import com.smokefree.program.domain.repo.PlanTemplateRepo;
import com.smokefree.program.domain.service.QuizService;
import com.smokefree.program.domain.service.quiz.SeverityRuleService;
import com.smokefree.program.web.dto.onboarding.BaselineResultRes;
import com.smokefree.program.web.dto.onboarding.PlanOption;
import com.smokefree.program.web.dto.quiz.QuizAnswerReq;
import com.smokefree.program.web.dto.quiz.QuizAnswerRes;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingFlowServiceImpl implements OnboardingFlowService {

    private final QuizService quizService;
    private final SeverityRuleService severityRules;
    private final PlanTemplateRepo planTemplateRepo;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public BaselineResultRes submitBaselineAndRecommend(UUID userId,
                                                        QuizAnswerReq req,
                                                        String userTier) {
        // 1) Tính điểm + severity bằng QuizService
        QuizAnswerRes quizRes = quizService.submitAnswers(userId, req, userTier);
        int total = quizRes.total();
        SeverityLevel sev = quizRes.severity();

        // 2) Map severity -> code template: L1_30D / L2_45D / L3_60D
        String recommendedCode = severityRules.recommendPlanCode(sev);

        // 3) Lấy 3 template theo seed (có thể đã có order theo level + code)
        List<String> codes = List.of("L1_30D", "L2_45D", "L3_60D");
        List<PlanTemplate> all = planTemplateRepo.findAllOrderByLevelCode();
        List<PlanTemplate> templates = all.stream()
                .filter(t -> codes.contains(t.getCode()))
                .toList();

        // 4) Lấy id template recommended (nhẹ DB payload)
        UUID recommendedId = planTemplateRepo.findIdByCode(recommendedCode)
                .orElseThrow(() -> new NoSuchElementException(
                        "Recommended template not found: " + recommendedCode));

        // 5) Map ra list PlanOption
        List<PlanOption> options = templates.stream()
                .map(t -> new PlanOption(
                        t.getId(),
                        t.getCode(),
                        t.getName(),
                        t.getTotalDays(),
                        t.getId().equals(recommendedId)
                ))
                .toList();

        return new BaselineResultRes(
                total,
                sev,
                recommendedId,
                recommendedCode,
                options
        );
    }
}
