package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.EnrollmentService;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.enrollment.EnrollmentRes;
import com.smokefree.program.web.dto.enrollment.StartEnrollmentReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService service;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public EnrollmentRes start(@RequestBody @Valid StartEnrollmentReq req) {
        return service.startTrialOrPaid(SecurityUtil.currentUserId(), req);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<EnrollmentRes> myEnrollments() {
        return service.listByUser(SecurityUtil.currentUserId());
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    public void complete(@PathVariable UUID id) {
        service.complete(SecurityUtil.currentUserId(), id);
    }
}

