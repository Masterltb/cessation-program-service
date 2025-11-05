package com.smokefree.program.web.controller;

import com.smokefree.program.domain.model.StepAssignment;
import com.smokefree.program.domain.model.StepStatus;

import com.smokefree.program.domain.service.smoke.StepAssignmentService;
import com.smokefree.program.web.dto.step.CreateStepAssignmentReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
// HỖ TRỢ CẢ HAI PATH để tránh 404 khi bạn gọi nhầm:
@RequestMapping({"/api/programs/{programId}/steps",
        "/api/programs/{programId}/step-assignments"})
public class StepAssignmentController {

    private final StepAssignmentService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<StepAssignment> list(@PathVariable UUID programId) {
        log.info("[Step] LIST programId={}", programId);
        return service.listByProgram(programId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public StepAssignment get(@PathVariable UUID programId, @PathVariable UUID id) {
        log.info("[Step] GET programId={}, id={}", programId, id);
        return service.getOne(programId, id);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public StepAssignment create(@PathVariable UUID programId,
                                 @RequestBody @Valid CreateStepAssignmentReq req) {
        log.info("[Step] CREATE programId={}, body={}", programId, req);
        return service.create(programId, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public StepAssignment updateStatus(@PathVariable UUID programId,
                                       @PathVariable UUID id,
                                       @RequestParam StepStatus status) {
        log.info("[Step] UPDATE-STATUS programId={}, id={}, status={}", programId, id, status);
        return service.updateStatus(programId, id, status);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public void delete(@PathVariable UUID programId, @PathVariable UUID id) {
        log.info("[Step] DELETE programId={}, id={}", programId, id);
        service.delete(programId, id);
    }
}
