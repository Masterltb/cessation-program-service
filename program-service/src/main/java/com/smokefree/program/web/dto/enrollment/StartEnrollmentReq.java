package com.smokefree.program.web.dto.enrollment;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartEnrollmentReq(
        @NotNull UUID planTemplateId,
        Boolean trial // true = trial 7 ngày; null/false = trả phí (sẽ kiểm tra subscription)
) {}
