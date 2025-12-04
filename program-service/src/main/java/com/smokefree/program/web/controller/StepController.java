package com.smokefree.program.web.controller;

import com.smokefree.program.domain.model.StepAssignment;
import com.smokefree.program.domain.model.StepStatus;
import com.smokefree.program.domain.service.smoke.StepAssignmentService;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.step.CreateStepAssignmentReq;
import com.smokefree.program.web.dto.step.RescheduleStepReq;
import com.smokefree.program.web.dto.step.UpdateStepStatusReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * StepController - Quản lý toàn bộ bài tập (steps) của program.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/programs/{programId}/steps")
public class StepController {

    private final StepAssignmentService service;
    private final Clock clock;

    // --- LIST & GET ---

    @GetMapping
    public Object list(@PathVariable UUID programId,
                       @RequestParam(name = "page", required = false) Integer page,
                       @RequestParam(name = "size", required = false) Integer size) {
        log.info("[Step] LIST programId={}", programId);
        if (page != null && size != null) {
            return service.listByProgram(programId, page, size);
        }
        return service.listByProgram(programId);
    }

    @GetMapping("/{id}")
    public StepAssignment get(@PathVariable UUID programId, @PathVariable UUID id) {
        log.info("[Step] GET programId={}, id={}", programId, id);
        return service.getOne(programId, id);
    }

    /**
     * Lấy danh sách step của ngày hôm nay.
     */
    @GetMapping("/today")
    public List<StepAssignment> getTodaySteps(@PathVariable UUID programId) {
        UUID userId = SecurityUtil.requireUserId();
        log.info("[Step] Get TODAY steps for program {} user {}", programId, userId);

        LocalDate todayUtc = LocalDate.now(clock);
        return service.listByProgramAndDate(programId, todayUtc);
    }

    /**
     * [DEBUG ONLY] Lấy step cho một ngày cụ thể được chỉ định.
     * Endpoint này chỉ tồn tại khi ứng dụng chạy với profile 'dev'.
     */
    @GetMapping("/debug/by-date/{date}")
    @Profile("dev") // Chỉ kích hoạt endpoint này trong môi trường dev
    public List<StepAssignment> getStepsForSpecificDate(
            @PathVariable UUID programId,
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.warn("[DEBUG] Getting steps for program {} on simulated date: {}", programId, date);
        return service.listByProgramAndDate(programId, date);
    }

    // --- CREATE & MANAGE ---

    @PostMapping
    public StepAssignment create(@PathVariable UUID programId,
                                 @RequestBody @Valid CreateStepAssignmentReq req) {
        log.info("[Step] CREATE programId={}, body={}", programId, req);
        return service.create(programId, req);
    }

    @PatchMapping("/{id}/status")
    public void updateStatus(@PathVariable("programId") UUID programId,
                             @PathVariable("id") UUID assignmentId,
                             @RequestBody UpdateStepStatusReq req) {

        UUID userId = SecurityUtil.requireUserId();
        service.updateStatus(userId, programId, assignmentId, req.status(), req.note());
    }

    @PostMapping("/{id}/skip")
    public StepAssignment skipStep(@PathVariable UUID programId, @PathVariable UUID id) {
        UUID userId = SecurityUtil.requireUserId();
        log.info("[Step] SKIP step {} program {}", id, programId);
        service.updateStatus(userId, programId, id, StepStatus.SKIPPED, "User skipped");
        return service.getOne(programId, id);
    }

    @PatchMapping("/{id}/reschedule")
    public StepAssignment rescheduleStep(@PathVariable UUID programId,
                                         @PathVariable UUID id,
                                         @RequestBody RescheduleStepReq req) {
        SecurityUtil.requireUserId();
        log.info("[Step] RESCHEDULE step {} to {}", id, req.newScheduledAt());
        return service.reschedule(programId, id, req.newScheduledAt());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID programId, @PathVariable UUID id) {
        log.info("[Step] DELETE programId={}, id={}", programId, id);
        service.delete(programId, id);
    }
}