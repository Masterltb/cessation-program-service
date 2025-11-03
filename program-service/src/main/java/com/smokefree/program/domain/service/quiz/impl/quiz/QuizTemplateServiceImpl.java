package com.smokefree.program.domain.service.quiz.impl.quiz;


import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.QuizTemplateRepository;
import com.smokefree.program.domain.service.quiz.QuizTemplateService;
import com.smokefree.program.web.dto.quiz.template.TemplateUpsertReq;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizTemplateServiceImpl implements QuizTemplateService {

    private final QuizTemplateRepository templateRepo;

    @Override
    public QuizTemplate createSystemTemplate(TemplateUpsertReq req, UUID adminId) {
        return createTemplate(req, "system", adminId);
    }

    @Override
    public QuizTemplate createCoachTemplate(TemplateUpsertReq req, UUID coachId) {
        return createTemplate(req, "coach", coachId);
    }

    private QuizTemplate createTemplate(TemplateUpsertReq req, String scope, UUID ownerId) {
        QuizTemplate t = new QuizTemplate();
        t.setId(UUID.randomUUID());
        t.setName(req.name());
        t.setLanguageCode(req.languageCode());
        t.setVersion(1);
        t.setStatus(QuizTemplateStatus.DRAFT);
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());

        // NEW
        t.setScope(scope);
        t.setOwnerId(ownerId);

        if (req.questions() != null) {
            for (var q : req.questions()) {
                var qid = new QuizTemplateQuestionId();
                qid.setTemplateId(t.getId());
                qid.setQuestionNo(q.questionNo());

                var qq = new QuizTemplateQuestion();
                qq.setId(qid);
                qq.setTemplate(t);
                qq.setText(q.text());

                if (q.choices() != null) {
                    for (var c : q.choices()) {
                        var cid = new QuizChoiceLabelId();
                        cid.setQuestionId(qid);         // <— thay vì setTemplateId / setQuestionNo
                        cid.setScore(c.score());

                        var cc = new QuizChoiceLabel();
                        cc.setId(cid);
                        cc.setQuestion(qq);
                        cc.setLabel(c.label());

                        qq.getChoiceLabels().add(cc); // tên collection trong entity question
                    }
                }

                t.getQuestions().add(qq);
            }
        }
        return templateRepo.save(t);
    }

    @Override
    public QuizTemplate publish(UUID templateId, UUID actorId) {
        QuizTemplate t = templateRepo.findById(templateId).orElseThrow();
        t.setStatus(QuizTemplateStatus.PUBLISHED);
        t.setPublishedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        return templateRepo.save(t);
    }

    @Override
    @Transactional
    public QuizTemplate cloneFromGlobal(UUID templateId, UUID coachId) {
        QuizTemplate src = templateRepo.findById(templateId).orElseThrow();

        if (!"system".equalsIgnoreCase(src.getScope())) {
            throw new IllegalStateException("Only system templates can be cloned.");
        }

        var req = new TemplateUpsertReq(
                src.getName() + " (Coach Copy)",
                src.getLanguageCode(),
                src.getQuestions().stream()
                        .sorted(Comparator.comparing(q -> q.getId().getQuestionNo()))
                        .map(q -> new TemplateUpsertReq.QuestionReq(
                                q.getId().getQuestionNo(),
                                q.getText(),
                                q.getChoiceLabels().stream() // dùng đúng tên collection
                                        .sorted(Comparator.comparing(c -> c.getId().getScore()))
                                        .map(c -> new TemplateUpsertReq.ChoiceReq(c.getId().getScore(), c.getLabel()))
                                        .toList()
                        ))
                        .toList()
        );
        return createCoachTemplate(req, coachId);
    }

}