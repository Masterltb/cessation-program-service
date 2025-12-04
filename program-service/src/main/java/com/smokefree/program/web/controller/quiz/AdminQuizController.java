package com.smokefree.program.web.controller.quiz;

import com.smokefree.program.domain.model.QuizTemplate;
import com.smokefree.program.domain.service.quiz.AdminQuizService;
import com.smokefree.program.web.dto.quiz.admin.CreateFullQuizReq;
import com.smokefree.program.web.dto.quiz.admin.QuestionDto;
import com.smokefree.program.web.dto.quiz.admin.QuizTemplateDetailRes;
import com.smokefree.program.web.dto.quiz.admin.QuizTemplateSummaryRes;
import com.smokefree.program.web.dto.quiz.admin.UpdateQuizContentReq;
import com.smokefree.program.web.dto.quiz.admin.UpdateFullQuizReq;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/admin/quizzes")
@RequiredArgsConstructor
public class AdminQuizController {

    private final AdminQuizService adminQuizService;

    @PostMapping
    public ResponseEntity<?> createFullQuiz(@Valid @RequestBody CreateFullQuizReq req) {
        var tpl = adminQuizService.createFullQuiz(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", tpl.getId(), "message", "Quiz '" + tpl.getName() + "' created successfully."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFullQuiz(@PathVariable UUID id, @Valid @RequestBody UpdateFullQuizReq req) {
        adminQuizService.updateFullQuiz(id, req);
        return ResponseEntity.ok(Map.of("message", "Quiz template updated successfully with " + req.questions().size() + " questions."));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishTemplate(@PathVariable UUID id) {
        adminQuizService.publishTemplate(id);
        return ResponseEntity.ok(Map.of("message", "Published successfully"));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<?> archiveTemplate(@PathVariable UUID id) {
        adminQuizService.archiveTemplate(id);
        return ResponseEntity.ok(Map.of("message", "Archived successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable UUID id) {
        adminQuizService.deleteTemplate(id);
        return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
    }

    @PutMapping("/{id}/content")
    public ResponseEntity<?> updateContent(@PathVariable UUID id,
                                           @Valid @RequestBody UpdateQuizContentReq req) {
        adminQuizService.updateContent(id, req);
        return ResponseEntity.ok(Map.of("message", "Quiz content updated successfully with " + req.questions().size() + " questions."));
    }

    @GetMapping
    @Transactional(Transactional.TxType.SUPPORTS)
    public ResponseEntity<?> listTemplates() {
        var list = adminQuizService.listAll().stream()
                .sorted(Comparator.comparing(QuizTemplate::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toSummary)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Transactional(Transactional.TxType.SUPPORTS)
    public ResponseEntity<QuizTemplateDetailRes> detail(@PathVariable UUID id) {
        QuizTemplate tpl = adminQuizService.getDetail(id);
        QuizTemplateDetailRes res = new QuizTemplateDetailRes(
                tpl.getId(),
                tpl.getCode(),
                tpl.getName(),
                tpl.getVersion(),
                tpl.getStatus() != null ? tpl.getStatus().name() : null,
                tpl.getQuestions().stream()
                        .sorted(Comparator.comparing(q -> q.getId().getQuestionNo()))
                        .map(q -> new QuestionDto(
                                q.getId().getQuestionNo(),
                                q.getQuestionText(),
                                q.getType(),
                                q.getExplanation(),
                                q.getChoiceLabels().stream()
                                        .sorted(Comparator.comparing(c -> c.getId().getLabelCode()))
                                        .map(c -> new com.smokefree.program.web.dto.quiz.admin.ChoiceDto(
                                                c.getId().getLabelCode(),
                                                c.getLabelText(),
                                                c.isCorrect(),
                                                c.getWeight()
                                        ))
                                        .collect(Collectors.toList())
                        ))
                        .toList()
        );
        return ResponseEntity.ok(res);
    }

    private QuizTemplateSummaryRes toSummary(QuizTemplate tpl) {
        int questionCount = tpl.getQuestions() == null ? 0 : tpl.getQuestions().size();
        return new QuizTemplateSummaryRes(
                tpl.getId(),
                tpl.getCode(),
                tpl.getName(),
                tpl.getVersion(),
                tpl.getStatus() != null ? tpl.getStatus().name() : null,
                questionCount,
                tpl.getCreatedAt()
        );
    }
}
