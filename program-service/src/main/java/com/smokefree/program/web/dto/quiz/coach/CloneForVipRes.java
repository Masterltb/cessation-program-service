// src/main/java/com/smokefree/program/web/dto/quiz/coach/CloneForVipRes.java
package com.smokefree.program.web.dto.quiz.coach;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CloneForVipRes(
        UUID templateId,
        UUID assignmentId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) OffsetDateTime expiresAt
) {}
