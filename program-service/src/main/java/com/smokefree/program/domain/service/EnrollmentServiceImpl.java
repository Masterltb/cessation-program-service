package com.smokefree.program.domain.service.impl;

import com.smokefree.program.domain.service.EnrollmentService;
import com.smokefree.program.web.dto.enrollment.EnrollmentRes;
import com.smokefree.program.web.dto.enrollment.StartEnrollmentReq;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    @Override
    public EnrollmentRes startTrialOrPaid(UUID userId, StartEnrollmentReq req) {
        // TODO: validate user tier / entitlement, check not running active program, persist to DB
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Instant trialUntil = Boolean.TRUE.equals(req.trial()) ? now.plus(7, ChronoUnit.DAYS) : null;

        return new EnrollmentRes(
                id, userId, req.planTemplateId(),
                "LIGHT_30D", // TODO: lấy từ repo
                "ACTIVE",
                now,
                null,
                trialUntil
        );
    }

    @Override
    public List<EnrollmentRes> listByUser(UUID userId) {
        // TODO: query DB
        return List.of();
    }

    @Override
    public void complete(UUID userId, UUID enrollmentId) {
        // TODO: chuyển status → COMPLETED, set endAt, ghi lịch sử
    }
}
