// src/main/java/com/smokefree/program/web/dto/quiz/coach/CloneForVipReq.java
package com.smokefree.program.web.dto.quiz.coach;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CloneForVipReq(
        String newName,
        @JsonFormat(shape = JsonFormat.Shape.STRING) OffsetDateTime expiresAt,
        String note
) {}
