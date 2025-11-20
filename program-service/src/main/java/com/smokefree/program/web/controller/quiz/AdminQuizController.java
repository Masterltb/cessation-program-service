// src/main/java/com/smokefree/program/web/controller/quiz/AdminQuizController.java
package com.smokefree.program.web.controller.quiz;

import com.smokefree.program.domain.model.AssignmentScope;
import com.smokefree.program.domain.service.quiz.QuizAssignmentService;
import com.smokefree.program.domain.service.quiz.QuizTemplateService;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.quiz.assignment.AssignmentReq;
import com.smokefree.program.web.dto.quiz.template.TemplateRes;
import com.smokefree.program.web.dto.quiz.template.TemplateUpsertReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// src/main/java/com/smokefree/program/web/controller/quiz/AdminQuizController.java

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class AdminQuizController {

    private final QuizTemplateService templateService;
    private final QuizAssignmentService assignmentService;

    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public TemplateRes createSystem(@RequestBody @Valid TemplateUpsertReq req) {
        UUID userId = getUserId();
        var t = templateService.createSystemTemplate(req, userId);
        return new TemplateRes(t.getId(), t.getName(), t.getVersion(), t.getStatus().name());
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public void assign(@RequestBody @Valid AssignmentReq req) {
        UUID userId = getUserId();
        assignmentService.assignToPrograms(
                req.templateId(),
                req.programIds(),
                req.everyDays() == null ? 5 : req.everyDays(),
                userId,
                null    // scope: admin → dùng default (DAY) trong service
        );
    }

    private UUID getUserId() {
        return com.smokefree.program.util.SecurityUtil.requireUserId();
    }
}
