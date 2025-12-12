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

/**
 * Controller quản lý các mẫu câu hỏi (Quiz Templates) dành cho Admin.
 * Cung cấp các API để tạo, sửa, xóa, xuất bản, lưu trữ và xem chi tiết các bài kiểm tra.
 */
@RestController
@RequestMapping("/v1/admin/quizzes")
@RequiredArgsConstructor
public class AdminQuizController {

    private final AdminQuizService adminQuizService;

    /**
     * Đưa một mẫu câu hỏi đã xuất bản (PUBLISHED) trở lại trạng thái nháp (DRAFT).
     *
     * @param id ID của mẫu câu hỏi.
     * @return Thông báo thành công.
     */
    @PostMapping("/{id}/draft")
    public ResponseEntity<?> revertToDraft(@PathVariable UUID id) {
        adminQuizService.revertToDraft(id);
        return ResponseEntity.ok(Map.of("message", "Reverted to DRAFT successfully"));
    }

    /**
     * Tạo mới một mẫu câu hỏi với đầy đủ thông tin (câu hỏi, lựa chọn).
     *
     * @param req DTO chứa thông tin tạo mới.
     * @return ID và thông báo thành công.
     */
    @PostMapping
    public ResponseEntity<?> createFullQuiz(@Valid @RequestBody CreateFullQuizReq req) {
        var tpl = adminQuizService.createFullQuiz(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", tpl.getId(), "message", "Quiz '" + tpl.getName() + "' created successfully."));
    }

    /**
     * Cập nhật toàn bộ thông tin của một mẫu câu hỏi (chỉ áp dụng cho trạng thái DRAFT).
     *
     * @param id  ID của mẫu câu hỏi.
     * @param req DTO chứa thông tin cập nhật.
     * @return Thông báo thành công.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFullQuiz(@PathVariable UUID id, @Valid @RequestBody UpdateFullQuizReq req) {
        adminQuizService.updateFullQuiz(id, req);
        return ResponseEntity.ok(Map.of("message", "Quiz template updated successfully with " + req.questions().size() + " questions."));
    }

    /**
     * Xuất bản mẫu câu hỏi, chuyển trạng thái sang PUBLISHED để có thể sử dụng.
     *
     * @param id ID của mẫu câu hỏi.
     * @return Thông báo thành công.
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishTemplate(@PathVariable UUID id) {
        adminQuizService.publishTemplate(id);
        return ResponseEntity.ok(Map.of("message", "Published successfully"));
    }

    /**
     * Lưu trữ mẫu câu hỏi, chuyển trạng thái sang ARCHIVED (không còn sử dụng nhưng vẫn giữ lịch sử).
     *
     * @param id ID của mẫu câu hỏi.
     * @return Thông báo thành công.
     */
    @PutMapping("/{id}/archive")
    public ResponseEntity<?> archiveTemplate(@PathVariable UUID id) {
        adminQuizService.archiveTemplate(id);
        return ResponseEntity.ok(Map.of("message", "Archived successfully"));
    }

    /**
     * Xóa vĩnh viễn một mẫu câu hỏi (chỉ khi chưa được sử dụng).
     *
     * @param id ID của mẫu câu hỏi.
     * @return Thông báo thành công.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable UUID id) {
        adminQuizService.deleteTemplate(id);
        return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
    }

    /**
     * Cập nhật nội dung câu hỏi/lựa chọn của mẫu câu hỏi (giữ nguyên các thông tin khác).
     *
     * @param id  ID của mẫu câu hỏi.
     * @param req DTO chứa nội dung cập nhật.
     * @return Thông báo thành công.
     */
    @PutMapping("/{id}/content")
    public ResponseEntity<?> updateContent(@PathVariable UUID id,
                                           @Valid @RequestBody UpdateQuizContentReq req) {
        adminQuizService.updateContent(id, req);
        return ResponseEntity.ok(Map.of("message", "Quiz content updated successfully with " + req.questions().size() + " questions."));
    }

    /**
     * Lấy danh sách tóm tắt tất cả các mẫu câu hỏi hiện có.
     *
     * @return Danh sách các mẫu câu hỏi (summary).
     */
    @GetMapping
    @Transactional(Transactional.TxType.SUPPORTS)
    public ResponseEntity<?> listTemplates() {
        var list = adminQuizService.listAll().stream()
                .sorted(Comparator.comparing(QuizTemplate::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toSummary)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Lấy thông tin chi tiết của một mẫu câu hỏi cụ thể (bao gồm cả câu hỏi và đáp án).
     *
     * @param id ID của mẫu câu hỏi.
     * @return Chi tiết mẫu câu hỏi.
     */
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

    /**
     * Chuyển đổi từ entity QuizTemplate sang DTO tóm tắt.
     */
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
