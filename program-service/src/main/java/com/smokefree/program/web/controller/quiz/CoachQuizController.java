package com.smokefree.program.web.controller.quiz;

import com.smokefree.program.domain.model.AssignmentScope;
import com.smokefree.program.domain.service.quiz.QuizAssignmentService;
import com.smokefree.program.domain.service.quiz.QuizTemplateService;
import com.smokefree.program.web.dto.quiz.assignment.AssignmentReq;
import com.smokefree.program.web.dto.quiz.template.TemplateRes;
import com.smokefree.program.web.dto.quiz.template.TemplateUpsertReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/coach/quiz")
@RequiredArgsConstructor
public class CoachQuizController {

    private final QuizTemplateService templateService;
    private final QuizAssignmentService assignmentService;

    @PostMapping("/templates")
    @PreAuthorize("hasRole('COACH')")
    public TemplateRes createCoach(@RequestBody @Valid TemplateUpsertReq req) {
        UUID userId = getUserId();
        var t = templateService.createCoachTemplate(req, userId);
        return new TemplateRes(t.getId(), t.getName(), t.getVersion(), t.getStatus().name());
    }

    // Gợi ý path: "/templates/{id}/clone"
    @PostMapping("/templates/{id}:clone")
    @PreAuthorize("hasRole('COACH')")
    public TemplateRes cloneFromGlobal(@PathVariable UUID id) {
        UUID userId = getUserId();
        var t = templateService.cloneFromGlobal(id, userId);
        return new TemplateRes(t.getId(), t.getName(), t.getVersion(), t.getStatus().name());
    }

    @PostMapping("/assignments/coach")
    @PreAuthorize("hasRole('COACH')")
    public void assignForOwnPrograms(@RequestBody @Valid AssignmentReq req) {
        UUID coachId = getUserId();
        assignmentService.assignToPrograms(
                req.templateId(),
                req.programIds(),
                req.everyDays() == null ? 5 : req.everyDays(),
                coachId,
                AssignmentScope.DAY   // hoặc WEEK/PROGRAM tuỳ rule lặp
        );
    }

    private UUID getUserId() {
        return com.smokefree.program.util.SecurityUtil.requireUserId();
    }
}
