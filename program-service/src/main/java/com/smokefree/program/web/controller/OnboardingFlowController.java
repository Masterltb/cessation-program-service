package com.smokefree.program.web.controller;

import com.smokefree.program.domain.repo.QuizTemplateRepository;
import com.smokefree.program.domain.service.onboarding.OnboardingFlowService;
import com.smokefree.program.util.EntitlementUtil;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.onboarding.BaselineResultRes;
import com.smokefree.program.web.dto.quiz.QuizAnswerReq;
import com.smokefree.program.web.dto.quiz.attempt.OpenAttemptRes;
import com.smokefree.program.web.error.NotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller quản lý luồng Onboarding (nhập môn) cho người dùng mới.
 * Cung cấp API để lấy bài đánh giá đầu vào và nộp kết quả để nhận lộ trình cai thuốc.
 */
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingFlowController {

    private static final String ONBOARDING_TEMPLATE_CODE = "ONBOARDING_ASSESSMENT";

    private final OnboardingFlowService onboardingFlowService;
    private final QuizTemplateRepository quizTemplateRepository;

    /**
     * Xử lý việc nộp bài đánh giá đầu vào (Baseline Quiz).
     * Dựa trên câu trả lời, hệ thống sẽ đề xuất lộ trình phù hợp.
     *
     * @param req  Đối tượng chứa danh sách câu trả lời của người dùng.
     * @return Kết quả đánh giá và lộ trình được đề xuất.
     */
    @PostMapping("/baseline")
    public BaselineResultRes baseline(@RequestBody @Valid QuizAnswerReq req) {
        UUID userId = SecurityUtil.requireUserId();
        String tier = EntitlementUtil.currentTier();
        return onboardingFlowService.submitBaselineAndRecommend(userId, req, tier);
    }

    /**
     * Lấy nội dung bài đánh giá đầu vào (câu hỏi, lựa chọn) cho user chưa có Program.
     * Dựa trên mã template cố định là "ONBOARDING_ASSESSMENT".
     *
     * @return Thông tin chi tiết về bài quiz (OpenAttemptRes) để hiển thị lên UI.
     */
    @GetMapping("/baseline/quiz")
    @Transactional(Transactional.TxType.SUPPORTS)
    public OpenAttemptRes getBaselineQuiz() {
        // Tìm template theo mã code cố định
        var template = quizTemplateRepository.findByCode(ONBOARDING_TEMPLATE_CODE)
                .orElseThrow(() -> new NotFoundException(
                        "Onboarding quiz template not found: " + ONBOARDING_TEMPLATE_CODE));

        // Map danh sách câu hỏi và lựa chọn sang DTO
        var questions = template.getQuestions().stream()
                .sorted(Comparator.comparing(q -> q.getId().getQuestionNo()))
                .map(q -> new OpenAttemptRes.QuestionView(
                        q.getId().getQuestionNo(),
                        q.getQuestionText(),
                        q.getChoiceLabels().stream()
                                .sorted(Comparator.comparing(c -> c.getId().getLabelCode()))
                                .collect(Collectors.toMap(
                                        c -> c.getId().getLabelCode(),
                                        com.smokefree.program.domain.model.QuizChoiceLabel::getLabelText,
                                        (a, b) -> a,
                                        LinkedHashMap::new
                                ))
                ))
                .toList();

        return new OpenAttemptRes(
                null, // Không có attemptId vì đây là quiz tĩnh, chưa tạo lượt thi
                template.getId(),
                template.getVersion(),
                questions
        );
    }
}