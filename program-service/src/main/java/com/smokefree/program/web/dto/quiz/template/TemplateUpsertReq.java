// src/main/java/com/smokefree/program/web/dto/quiz/template/TemplateUpsertReq.java
package com.smokefree.program.web.dto.quiz.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TemplateUpsertReq(
        @NotBlank
        String name,

        @NotBlank
        String languageCode,

        @NotNull
        List<QuestionUpsertReq> questions
) {}
