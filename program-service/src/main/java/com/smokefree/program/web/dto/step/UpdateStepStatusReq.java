package com.smokefree.program.web.dto.step;

import com.smokefree.program.domain.model.StepStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStepStatusReq(@NotNull StepStatus status) {}
