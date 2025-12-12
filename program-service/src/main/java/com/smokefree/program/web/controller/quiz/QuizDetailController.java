package com.smokefree.program.web.controller.quiz;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller xử lý các yêu cầu chi tiết về lịch sử và kết quả bài kiểm tra.
 * Hiện tại các endpoint này chưa được triển khai (trả về 501 Not Implemented).
 */
@RestController
@RequestMapping("/me/quiz")
public class QuizDetailController {

    /**
     * Lấy lịch sử các lần làm bài của một mẫu câu hỏi cụ thể.
     *
     * @param templateId ID của mẫu câu hỏi.
     * @param size       Số lượng bản ghi tối đa trả về.
     * @return Danh sách lịch sử làm bài.
     */
    @GetMapping("/{templateId}/attempts")
    public List<QuizAttemptHistoryRes> getAttemptHistory(@PathVariable UUID templateId,
                                                         @RequestParam(defaultValue = "10") int size) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Quiz attempt history not implemented");
    }

    /**
     * Lấy chi tiết kết quả của một lần làm bài cụ thể.
     *
     * @param templateId ID của mẫu câu hỏi.
     * @param attemptId  ID của lần làm bài.
     * @return Chi tiết kết quả làm bài.
     */
    @GetMapping("/{templateId}/attempts/{attemptId}")
    public QuizAttemptDetailRes getAttemptDetail(@PathVariable UUID templateId,
                                                 @PathVariable UUID attemptId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Quiz attempt detail not implemented");
    }

    /**
     * Cho phép người dùng làm lại một bài kiểm tra (nếu chính sách cho phép).
     *
     * @param templateId ID của mẫu câu hỏi.
     * @return Thông tin lượt thi mới.
     */
    @PostMapping("/{templateId}/retry")
    public QuizAttemptRes retryQuiz(@PathVariable UUID templateId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Quiz retry not implemented");
    }
}

/**
 * DTO trả về tóm tắt lịch sử một lần làm bài.
 */
record QuizAttemptHistoryRes(
        UUID attemptId,
        Instant openedAt,
        Instant submittedAt,
        Integer totalScore,
        String severity
) {}

/**
 * DTO trả về chi tiết đầy đủ của một lần làm bài, bao gồm cả câu trả lời.
 */
record QuizAttemptDetailRes(
        UUID attemptId,
        UUID templateId,
        Integer totalScore,
        String severity,
        Integer version,
        List<QuizAnswerItemDetail> answers
) {}

/**
 * DTO chi tiết từng câu trả lời trong kết quả.
 */
record QuizAnswerItemDetail(
        Integer questionNo,
        String questionText,
        Integer answer,
        String answerText
) {}

/**
 * DTO trả về thông tin lượt thi (thường dùng khi bắt đầu hoặc retry).
 */
record QuizAttemptRes(
        UUID attemptId,
        UUID templateId,
        Integer version,
        List<Object> questions
) {}
