package com.smokefree.program.domain.service.quiz;



import com.smokefree.program.domain.model.QuizChoiceLabelId;
import com.smokefree.program.domain.model.QuizTemplate;
import com.smokefree.program.domain.model.QuizTemplateQuestionId;

import java.util.UUID;

public interface AdminQuizService {
    QuizTemplate createTemplate(String name);
    void publishTemplate(UUID templateId);
     QuizTemplateQuestionId addQuestion(
            UUID templateId,
            Integer orderNo,
            String text,
            String type,         // hoặc QuestionType type
            Integer points,
            String explanation);
     QuizChoiceLabelId addChoice(
            UUID templateId, Integer questionNo,
            String labelCode, String labelText,
            Boolean correct, Integer weight);
}
