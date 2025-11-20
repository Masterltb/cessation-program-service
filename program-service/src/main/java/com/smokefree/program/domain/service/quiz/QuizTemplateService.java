package com.smokefree.program.domain.service.quiz;


import com.smokefree.program.domain.model.QuizTemplate;
import com.smokefree.program.web.dto.quiz.template.TemplateUpsertReq;

import java.util.UUID;

public interface QuizTemplateService {
    QuizTemplate createSystemTemplate(TemplateUpsertReq req, UUID adminId);
    QuizTemplate createCoachTemplate(TemplateUpsertReq req, UUID coachId);
    QuizTemplate publish(UUID templateId, UUID actorId);           // draft -> published
    QuizTemplate cloneFromGlobal(UUID templateId, UUID coachId);   // clone scope=coach
}
