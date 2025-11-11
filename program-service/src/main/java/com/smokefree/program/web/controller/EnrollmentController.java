package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.EnrollmentService;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.enrollment.EnrollmentRes;
import com.smokefree.program.web.dto.enrollment.StartEnrollmentReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<EnrollmentRes> start(@RequestBody @Valid StartEnrollmentReq req) {
        EnrollmentRes res = service.startTrialOrPaid(SecurityUtil.currentUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnrollmentRes>> myEnrollments() {
        List<EnrollmentRes> list = service.listByUser(SecurityUtil.currentUserId());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> complete(@PathVariable UUID id) {
        service.complete(SecurityUtil.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}

