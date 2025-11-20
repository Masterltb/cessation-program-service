package com.smokefree.program.web.dto.quiz.assignment;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AssignmentReq(
        @NotNull
        UUID templateId,

        @NotEmpty
        List<UUID> programIds,

        Integer everyDays
) {}