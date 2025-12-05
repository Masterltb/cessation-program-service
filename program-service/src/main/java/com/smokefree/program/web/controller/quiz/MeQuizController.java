package com.smokefree.program.web.controller.quiz;

import com.smokefree.program.domain.service.quiz.QuizFlowService;
import com.smokefree.program.web.dto.quiz.answer.AnswerReq;
import com.smokefree.program.web.dto.quiz.attempt.DueItem;
import com.smokefree.program.web.dto.quiz.attempt.OpenAttemptRes;
import com.smokefree.program.web.dto.quiz.result.SubmitRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/me/quizzes")
@RequiredArgsConstructor
public class MeQuizController {

    private final QuizFlowService quizFlowService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listDueQuizzes(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup,
            @RequestHeader(value = "X-User-Tier", required = false) String userTier) {

        log.info("[MeQuiz] listDueQuizzes - userId: {}, group: {}", userId, userGroup);

        try {
            List<DueItem> dueItems = quizFlowService.listDue(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dueItems);
            response.put("count", dueItems.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[MeQuiz] Error listing due quizzes", e);
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{templateId}/open")
    public ResponseEntity<Map<String, Object>> openAttempt(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID templateId,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {

        log.info("[MeQuiz] openAttempt - userId: {}, templateId: {}", userId, templateId);

        try {
            OpenAttemptRes attempt = quizFlowService.openAttempt(userId, templateId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", attempt);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("[MeQuiz] Error opening attempt", e);
            // Nếu là lỗi hết hạn trial -> trả về 402 hoặc 403
            if (e instanceof com.smokefree.program.web.error.SubscriptionRequiredException) {
                 return buildErrorResponse(HttpStatus.PAYMENT_REQUIRED, e.getMessage());
            }
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{attemptId}/answer")
    public ResponseEntity<Map<String, Object>> saveAnswer(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID attemptId,
            @RequestBody AnswerReq request) {

        try {
            // Không cần templateId nữa
            quizFlowService.saveAnswer(userId, attemptId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Answer saved successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[MeQuiz] Error saving answer", e);
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<Map<String, Object>> submitQuiz(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID attemptId) {

        try {
            // Không cần templateId nữa
            SubmitRes result = quizFlowService.submit(userId, attemptId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[MeQuiz] Error submitting quiz", e);
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("status", status.value());
        return ResponseEntity.status(status).body(errorResponse);
    }
}