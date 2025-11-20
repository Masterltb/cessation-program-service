// src/main/java/com/smokefree/program/domain/service/EnrollmentServiceImpl.java
package com.smokefree.program.domain.service;

import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.ProgramStatus;
import com.smokefree.program.domain.repo.PlanTemplateRepo;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.web.dto.enrollment.EnrollmentRes;
import com.smokefree.program.web.dto.enrollment.StartEnrollmentReq;
import com.smokefree.program.web.error.ConflictException;
import com.smokefree.program.web.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final ProgramRepository programRepo;
    private final PlanTemplateRepo planTemplateRepo;
    private final ProgramCreationService programCreationService;
    @Override
    @Transactional
    public EnrollmentRes startTrialOrPaid(UUID userId, StartEnrollmentReq req) {
        programRepo.findFirstByUserIdAndStatusAndDeletedAtIsNull(userId, ProgramStatus.ACTIVE)
                .ifPresent(p -> { throw new ConflictException("User already has an ACTIVE program"); });

        UUID templateId = req.planTemplateId();
        String planCode = null;
        int planDays = 30; // default

        if (templateId != null) {
            var tpl = planTemplateRepo.findById(templateId)
                    .orElseThrow(() -> new NotFoundException("Plan template not found: " + templateId));
            planCode = tpl.getCode();
            planDays = tpl.getTotalDays();
        }

        Program p;
        if (Boolean.TRUE.equals(req.trial())) {
            // trial 7 ngày
            p = programCreationService.createTrialProgram(userId, planDays, 7, null);
        } else {
            // gói trả phí ngay
            p = programCreationService.createPaidProgram(userId, planDays, null);
        }

        p = programRepo.save(p);

        // đọc lại thời điểm hết trial từ entity
        Instant trialUntil = p.getTrialEndExpected();

        return new EnrollmentRes(
                p.getId(),
                p.getUserId(),
                templateId,
                planCode,
                p.getStatus().name(),
                p.getStartDate() == null
                        ? null
                        : p.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant(),
                null,          // endAt: hiện chưa lưu trong Program
                trialUntil     // trialUntil: chỉ khác null cho chương trình trial
        );
    }


    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentRes> listByUser(UUID userId) {
        List<Program> programs = programRepo.findAllByUserId(userId);
        programs.sort(Comparator.comparing(Program::getCreatedAt).reversed());

        return programs.stream().map(p -> new EnrollmentRes(
                p.getId(),
                p.getUserId(),
                null, // Program chưa lưu planTemplateId
                null, // Không suy ra được planCode từ DB
                p.getStatus().name(),
                p.getStartDate() == null ? null : p.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant(),
                null, // Program chưa có endAt
                p.getTrialEndExpected()
        )).toList();
    }

    @Override
    @Transactional
    public void complete(UUID userId, UUID enrollmentId) {
        Program p = programRepo.findByIdAndUserId(enrollmentId, userId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));

        if (p.getStatus() == ProgramStatus.COMPLETED) {
            throw new ConflictException("Enrollment already completed");
        }
        if (p.getStatus() == ProgramStatus.CANCELLED) {
            throw new ConflictException("Enrollment is cancelled");
        }

        p.setStatus(ProgramStatus.COMPLETED);
        programRepo.save(p);

        // Nếu muốn lưu thời điểm hoàn tất, hãy thêm cột endAt (Instant) vào Program + Flyway.
    }

    // ----------------- helpers -----------------

    private static String safeGetCode(Object template) {
        try { return (String) template.getClass().getMethod("getCode").invoke(template); }
        catch (Exception ignore) {
            try { return (String) template.getClass().getMethod("getName").invoke(template); }
            catch (Exception e) { return null; }
        }
    }

    private static Integer safeGetDays(Object template) {
        try {
            Object v = template.getClass().getMethod("getDays").invoke(template);
            return (v instanceof Integer i) ? i : null;
        } catch (Exception ignore) {
            // fallback: đoán từ code "xxx_30D|45D|60D"
            String code = safeGetCode(template);
            if (code == null) return null;
            if (code.contains("30")) return 30;
            if (code.contains("45")) return 45;
            if (code.contains("60")) return 60;
            return null;
        }
    }
}
