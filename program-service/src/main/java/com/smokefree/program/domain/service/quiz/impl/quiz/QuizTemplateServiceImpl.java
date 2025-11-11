package com.smokefree.program.domain.service.quiz.impl.quiz;


import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.QuizTemplateRepository;
import com.smokefree.program.domain.service.quiz.QuizTemplateService;
import com.smokefree.program.web.dto.quiz.template.TemplateUpsertReq;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

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
        t.setScope(
                scope == null
                        ? QuizTemplateScope.SYSTEM
                        : QuizTemplateScope.valueOf(scope.trim().toUpperCase())
        );
        t.setOwnerId(ownerId);
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());

        var questions = req.questions();
        if (questions != null) {
            for (var q : questions) {
                // ID câu hỏi = (templateId, questionNo)
                var qid = new QuizTemplateQuestionId(t.getId(), q.questionNo());

                var qq = new QuizTemplateQuestion();
                qq.setId(qid);
                qq.setTemplate(t);
                qq.setQuestionText(q.text());
                qq.setType(q.type());                // ✔ sửa: dùng enum trực tiếp
                qq.setPoints(q.points());
                qq.setExplanation(q.explanation());

                var choices = q.choices();
                if (choices != null) {
                    for (var c : choices) {
                        // ID choice = (templateId, questionNo, labelCode)
                        var cid = new QuizChoiceLabelId(t.getId(), q.questionNo(), c.labelCode());

                        var cc = new QuizChoiceLabel();
                        cc.setId(cid);
                        cc.setQuestion(qq);           // liên kết về câu hỏi (JoinColumns)
                        cc.setLabelText(c.labelText());
                        cc.setCorrect(Boolean.TRUE.equals(c.correct()));
                        cc.setWeight(c.weight());

                        qq.getChoiceLabels().add(cc);
                    }
                }

                t.getQuestions().add(qq);
            }
        }

        return templateRepo.save(t); // hoặc tplRepo.save(t) tùy tên bean bạn đang dùng
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
        QuizTemplate src = templateRepo.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        // chỉ cho phép clone template hệ thống
        QuizTemplateScope scope = Optional.ofNullable(src.getScope()).orElse(QuizTemplateScope.SYSTEM);
        if (scope != QuizTemplateScope.SYSTEM) {
            throw new IllegalStateException("Only system templates can be cloned.");
        }

        List<TemplateUpsertReq.QuestionUpsertReq> qReqs = src.getQuestions().stream()
                .sorted(Comparator.comparing(q -> q.getId().getQuestionNo()))
                .map(q -> new TemplateUpsertReq.QuestionUpsertReq(
                        q.getId().getQuestionNo(),
                        q.getQuestionText(),
                        q.getType(),
                        q.getPoints(),
                        q.getExplanation(),
                        q.getChoiceLabels().stream()
                                .sorted(Comparator.comparing(c -> c.getId().getLabelCode()))
                                .map(c -> new TemplateUpsertReq.ChoiceUpsertReq(
                                        c.getId().getLabelCode(),
                                        c.getLabelText(),
                                        c.isCorrect(),
                                        c.getWeight()
                                ))
                                .toList()
                ))
                .toList();

        TemplateUpsertReq req = new TemplateUpsertReq(
                src.getName() + " (Coach Copy)",
                src.getLanguageCode(),
                qReqs
        );

        return createCoachTemplate(req, coachId);
    }

}