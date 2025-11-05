package com.smokefree.program.web.dto.enrollment;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentRes(
        UUID id,
        UUID userId,
        UUID planTemplateId,
        String planCode,
        String status,         // ACTIVE|COMPLETED|CANCELLED
        Instant startAt,
        Instant endAt,
        Instant trialUntil
) {}
