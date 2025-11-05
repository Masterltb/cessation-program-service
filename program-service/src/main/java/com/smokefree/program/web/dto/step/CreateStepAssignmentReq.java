package com.smokefree.program.web.dto.step;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CreateStepAssignmentReq(
        @NotNull @Min(1) Integer stepNo,
        String note,
        OffsetDateTime eventAt // optional: nếu null service tự set
) {}
