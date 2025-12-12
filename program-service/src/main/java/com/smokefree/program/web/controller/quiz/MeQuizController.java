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

/**
 * Controller xử lý các thao tác liên quan đến bài kiểm tra của người dùng hiện tại (Me).
 * Cung cấp các API để lấy danh sách bài thi cần làm, bắt đầu làm bài, lưu câu trả lời và nộp bài.
 */
@Slf4j
@RestController
@RequestMapping("/v1/me/quizzes")
@RequiredArgsConstructor
public class MeQuizController {

    private final QuizFlowService quizFlowService;

    /**
     * Lấy danh sách các bài kiểm tra cần hoàn thành của người dùng.
     *
     * @param userId    ID của người dùng (từ header).
     * @param userGroup Nhóm người dùng (từ header, tùy chọn).
     * @param userTier  Hạng người dùng (từ header, tùy chọn).
     * @return Danh sách các bài kiểm tra cần làm (DueItems).
     */
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

    /**
     * Mở một lượt thi mới cho một bài kiểm tra cụ thể.
     *
     * @param userId     ID của người dùng.
     * @param templateId ID của mẫu bài kiểm tra.
     * @param userGroup  Nhóm người dùng.
     * @return Thông tin chi tiết về lượt thi vừa tạo.
     */
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

    /**
     * Lưu câu trả lời cho một câu hỏi trong lượt thi.
     *
     * @param userId    ID của người dùng.
     * @param attemptId ID của lượt thi.
     * @param request   Đối tượng chứa thông tin câu trả lời.
     * @return Phản hồi thành công.
     */
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

    /**
     * Nộp bài kiểm tra để chấm điểm và kết thúc lượt thi.
     *
     * @param userId    ID của người dùng.
     * @param attemptId ID của lượt thi.
     * @return Kết quả bài thi.
     */
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

    /**
     * Xây dựng phản hồi lỗi chuẩn hóa.
     *
     * @param status  Mã trạng thái HTTP.
     * @param message Thông báo lỗi.
     * @return ResponseEntity chứa thông tin lỗi.
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("status", status.value());
        return ResponseEntity.status(status).body(errorResponse);
    }
}