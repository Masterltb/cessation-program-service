// TemplateUpsertReq.java
package com.smokefree.program.web.dto.quiz.template;

import com.smokefree.program.domain.model.QuestionType;
import java.util.List;

public record TemplateUpsertReq(
        String name,
        String languageCode,
        List<QuestionUpsertReq> questions
) {
    public record QuestionUpsertReq(
            Integer questionNo,
            String text,
            QuestionType type,
            Integer points,
            String explanation,
            List<ChoiceUpsertReq> choices
    ) {}

    public record ChoiceUpsertReq(
            String labelCode,
            String labelText,
            Boolean correct,
            Integer weight
    ) {}
}
