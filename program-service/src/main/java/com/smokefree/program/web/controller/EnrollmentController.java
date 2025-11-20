// src/main/java/com/smokefree/program/web/controller/enrollment/EnrollmentController.java
package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.EnrollmentService;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.enrollment.EnrollmentRes;
import com.smokefree.program.web.dto.enrollment.StartEnrollmentReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * Start trial 7 ngày từ 1 plan template.
     * Frontend chỉ gọi endpoint này cho trial.
     */
    @PostMapping("/start-trial")
    @PreAuthorize("isAuthenticated()")
    public EnrollmentRes startTrial(@RequestBody @Valid StartEnrollmentReq req) {
        UUID userId = SecurityUtil.requireUserId();
        // ép trial = true để client không phá logic
        StartEnrollmentReq forced = new StartEnrollmentReq(req.planTemplateId(), true);
        return enrollmentService.startTrialOrPaid(userId, forced);
    }

    /**
     * Start enrollment "paid" (sau khi payment service xác nhận thanh toán OK).
     * Vẫn reuse logic trong EnrollmentServiceImpl nhưng trial=false.
     */
    @PostMapping("/start-paid")
    @PreAuthorize("isAuthenticated()")
    public EnrollmentRes startPaid(@RequestBody @Valid StartEnrollmentReq req) {
        UUID userId = SecurityUtil.requireUserId();
        StartEnrollmentReq forced = new StartEnrollmentReq(req.planTemplateId(), false);
        return enrollmentService.startTrialOrPaid(userId, forced);
    }

    // Nếu đang có POST /api/enrollments cũ, có thể giữ tạm & @Deprecated
}
