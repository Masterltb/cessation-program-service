package com.smokefree.program.web.controller.quiz;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Placeholder detail endpoints cho quiz: hiện chưa triển khai, trả 501.
 */
@RestController
@RequestMapping("/me/quiz")
public class QuizDetailController {

    @GetMapping("/{templateId}/attempts")

    public List<QuizAttemptHistoryRes> getAttemptHistory(@PathVariable UUID templateId,
                                                         @RequestParam(defaultValue = "10") int size) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Quiz attempt history not implemented");
    }

    @GetMapping("/{templateId}/attempts/{attemptId}")

    public QuizAttemptDetailRes getAttemptDetail(@PathVariable UUID templateId,
                                                 @PathVariable UUID attemptId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Quiz attempt detail not implemented");
    }

    @PostMapping("/{templateId}/retry")
    public QuizAttemptRes retryQuiz(@PathVariable UUID templateId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Quiz retry not implemented");
    }
}

record QuizAttemptHistoryRes(
        UUID attemptId,
        Instant openedAt,
        Instant submittedAt,
        Integer totalScore,
        String severity
) {}

record QuizAttemptDetailRes(
        UUID attemptId,
        UUID templateId,
        Integer totalScore,
        String severity,
        Integer version,
        List<QuizAnswerItemDetail> answers
) {}

record QuizAnswerItemDetail(
        Integer questionNo,
        String questionText,
        Integer answer,
        String answerText
) {}

record QuizAttemptRes(
        UUID attemptId,
        UUID templateId,
        Integer version,
        List<Object> questions
) {}
