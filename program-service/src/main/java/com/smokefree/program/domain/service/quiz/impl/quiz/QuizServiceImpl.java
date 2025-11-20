package com.smokefree.program.domain.service.quiz.impl.quiz;

import com.smokefree.program.domain.model.SeverityLevel;
import com.smokefree.program.domain.service.QuizService;
import com.smokefree.program.domain.service.quiz.SeverityRuleService;
import com.smokefree.program.web.dto.quiz.*;
import com.smokefree.program.web.error.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final SeverityRuleService severityRules;

    @Override
    public QuizAnswerRes submitAnswers(UUID userId, QuizAnswerReq req, String userTier) {
        if (req.answers() == null || req.answers().size() != 10)
            throw new ValidationException("Must provide exactly 10 answers");

        int total = req.answers().stream().mapToInt(a -> {
            int score = a.score();
            if (score < 1 || score > 5) {
                throw new ValidationException("Score must be 1..5 at q=" + a.q());
            }
            return score;
        }).sum();

        SeverityLevel sev = severityRules.fromScore(total);
        int planDays       = severityRules.recommendPlanDays(sev);

        String recTier = switch (sev) {
            case LOW      -> "basic";
            case MODERATE -> "premium";
            case HIGH     -> "vip";
        };
        List<String> alts = switch (sev) {
            case LOW      -> List.of("premium");
            case MODERATE -> List.of("vip");
            case HIGH     -> List.of("premium");
        };
        String reason = switch (sev) {
            case LOW      -> "Mức thấp — đủ Basic";
            case MODERATE -> "Trung bình — nên Premium";
            case HIGH     -> "Cao — cần VIP kèm coach";
        };

        return new QuizAnswerRes(
                total,
                sev,
                planDays,
                new Recommendation(recTier, alts, reason),
                new Trial(true, 7)
        );
    }

    @Override
    public SeverityLevel mapSeverity(int total) {
        return severityRules.fromScore(total);
    }

    @Override
    public int recommendPlanDays(SeverityLevel sev) {
        return severityRules.recommendPlanDays(sev);
    }
}
