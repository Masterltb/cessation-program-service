package com.smokefree.program.domain.service.smoke.impl;

import com.smokefree.program.domain.model.StepAssignment;
import com.smokefree.program.domain.model.StepStatus;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.repo.StepAssignmentRepository;
import com.smokefree.program.domain.service.smoke.StepAssignmentService;
import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.PlanTemplate;
import com.smokefree.program.domain.model.PlanStep;
import com.smokefree.program.domain.repo.PlanStepRepo;
import com.smokefree.program.web.dto.step.CreateStepAssignmentReq;
import com.smokefree.program.web.error.ForbiddenException;
import com.smokefree.program.web.error.NotFoundException;
import com.smokefree.program.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StepAssignmentServiceImpl implements StepAssignmentService {

    private final StepAssignmentRepository stepAssignmentRepository;
    private final PlanStepRepo planStepRepository;
    private final ProgramRepository programRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StepAssignment> listByProgram(UUID programId) {
        ensureProgramAccess(programId, true);
        log.debug("[StepAssignment] listByProgram: {}", programId);
        return stepAssignmentRepository.findByProgramIdOrderByStepNoAsc(programId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StepAssignment> listByProgramAndDate(UUID programId, LocalDate date) {
        ensureProgramAccess(programId, true);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        if (program.getStartDate() == null) {
            return List.of();
        }

        long offset = ChronoUnit.DAYS.between(program.getStartDate(), date);
        int plannedDay = (int) offset + 1;
        if (plannedDay < 1) {
            return List.of();
        }
        return stepAssignmentRepository.findByProgramIdAndPlannedDay(programId, plannedDay);
    }

    @Override
    @Transactional(readOnly = true)
    public StepAssignment getOne(UUID programId, UUID id) {
        ensureProgramAccess(programId, true);
        log.debug("[StepAssignment] getOne: programId={}, stepId={}", programId, id);
        return stepAssignmentRepository.findByIdAndProgramId(id, programId)
            .orElseThrow(() -> new NotFoundException("Step not found: " + id));
    }

    @Override
    @Transactional
    public StepAssignment create(UUID programId, CreateStepAssignmentReq req) {
        ensureProgramAccess(programId, false);
        log.info("[StepAssignment] create for programId: {}", programId);

        StepAssignment assignment = StepAssignment.builder()
            .id(UUID.randomUUID())
            .programId(programId)
            .stepNo(req.stepNo())
            .plannedDay(req.plannedDay())
            .status(StepStatus.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        return stepAssignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public void updateStatus(UUID userId, UUID programId, UUID assignmentId, StepStatus status, String note) {
        ensureProgramAccess(programId, false);
        log.info("[StepAssignment] updateStatus: programId={}, stepId={}, status={}", programId, assignmentId, status);

        StepAssignment step = stepAssignmentRepository.findByIdAndProgramId(assignmentId, programId)
            .orElseThrow(() -> new NotFoundException("Step not found: " + assignmentId));

        // Chỉ xử lý logic nếu trạng thái thực sự thay đổi và là COMPLETED
        if (step.getStatus() == status || status != StepStatus.COMPLETED) {
            step.setStatus(status);
            step.setNote(note);
            step.setUpdatedAt(Instant.now());
            stepAssignmentRepository.save(step);
            return;
        }

        step.setStatus(status);
        step.setNote(note);
        step.setUpdatedAt(Instant.now());
        step.setCompletedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));

        stepAssignmentRepository.save(step);

        // Gọi logic kiểm tra hoàn thành và cập nhật streak
        checkCompletionAndUpdateStreak(programId, step.getPlannedDay());
    }

    private void checkCompletionAndUpdateStreak(UUID programId, int plannedDay) {
        // Sử dụng phương thức truy vấn mới để kiểm tra hiệu quả
        long incompleteSteps = stepAssignmentRepository.countIncompleteStepsForDay(programId, plannedDay);

        if (incompleteSteps == 0) {
            log.info("[Streak] All steps for day {} in program {} are completed. Updating streak.", plannedDay, programId);

            Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found while updating streak: " + programId));

            // Tăng streak hiện tại và streak tốt nhất nếu cần
            int newStreak = program.getStreakCurrent() + 1;
            program.setStreakCurrent(newStreak);

            if (newStreak > program.getStreakBest()) {
                program.setStreakBest(newStreak);
            }

            programRepository.save(program);
            log.info("[Streak] Successfully updated for program {}. New streak: {}", programId, newStreak);
        } else {
            log.debug("[Streak] Program {} still has {} incomplete steps for day {}.", programId, incompleteSteps, plannedDay);
        }
    }

    @Override
    @Transactional
    public StepAssignment reschedule(UUID programId, UUID assignmentId, OffsetDateTime newScheduledAt) {
        ensureProgramAccess(programId, false);
        if (newScheduledAt == null) {
            throw new IllegalArgumentException("newScheduledAt is required");
        }
        StepAssignment step = stepAssignmentRepository.findByIdAndProgramId(assignmentId, programId)
                .orElseThrow(() -> new NotFoundException("Step not found: " + assignmentId));

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        if (step.getPlannedDay() != null && step.getPlannedDay() > program.getPlanDays()) {
            throw new IllegalArgumentException("Planned day exceeds program total days");
        }

        step.setScheduledAt(newScheduledAt);
        step.setUpdatedAt(Instant.now());
        return stepAssignmentRepository.save(step);
    }

    @Override
    @Transactional
    public void delete(UUID programId, UUID id) {
        ensureProgramAccess(programId, false);
        log.info("[StepAssignment] delete: programId={}, stepId={}", programId, id);

        StepAssignment step = stepAssignmentRepository.findByIdAndProgramId(id, programId)
            .orElseThrow(() -> new NotFoundException("Step not found: " + id));

        stepAssignmentRepository.delete(step);
    }

    @Override
    @Transactional
    public void createForProgramFromTemplate(Program program, PlanTemplate template) {
        log.info("[StepAssignment] Creating steps for program: {}, template: {}",
            program.getId(), template.getId());

        List<PlanStep> templateSteps = planStepRepository.findByTemplateIdOrderByDayNoAscSlotAsc(template.getId());
        log.debug("[StepAssignment] Found {} steps in template", templateSteps.size());

        int stepNo = 1;
        for (PlanStep planStep : templateSteps) {
            StepAssignment assignment = StepAssignment.builder()
                .id(UUID.randomUUID())
                .programId(program.getId())
                .stepNo(stepNo++)
                .plannedDay(planStep.getDayNo())
                .status(StepStatus.PENDING)
                .createdBy(program.getUserId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            LocalDate startDate = program.getStartDate();
            LocalDate scheduledDate = startDate.plusDays(planStep.getDayNo() - 1);
            assignment.setScheduledAt(
                scheduledDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
            );

            stepAssignmentRepository.save(assignment);
            log.debug("[StepAssignment] Created step {} for day {}",
                assignment.getStepNo(), planStep.getDayNo());
        }
        log.info("[StepAssignment] Successfully created {} step assignments", templateSteps.size());
    }

    private void ensureProgramAccess(UUID programId, boolean allowCoachWrite) {
        if (SecurityUtil.hasRole("ADMIN")) {
            return;
        }
        UUID userId = SecurityUtil.requireUserId();
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        boolean isOwner = program.getUserId().equals(userId);
        boolean isCoach = program.getCoachId() != null && program.getCoachId().equals(userId) && SecurityUtil.hasRole("COACH");
        if (!isOwner && !isCoach) {
            throw new ForbiddenException("Access denied for program " + programId);
        }
        if (isCoach && !allowCoachWrite) {
            throw new ForbiddenException("Coach cannot modify steps for program " + programId);
        }
    }
}
