package com.smokefree.program.domain.service.quiz.impl.quiz;


import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.QuizTemplateRepository;
import com.smokefree.program.domain.service.quiz.QuizTemplateService;
import com.smokefree.program.web.dto.quiz.template.ChoiceUpsertReq;
import com.smokefree.program.web.dto.quiz.template.QuestionUpsertReq;
import com.smokefree.program.web.dto.quiz.template.TemplateUpsertReq;
import com.smokefree.program.web.error.ConflictException;
import com.smokefree.program.web.error.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

import static java.util.Collections.emptyList;

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

    private QuizTemplate createTemplate(
            com.smokefree.program.web.dto.quiz.template.TemplateUpsertReq req,
            String scope,
            UUID ownerId
    ) {
        // Template (để @PrePersist tự set id)
        QuizTemplate t = new QuizTemplate();
        t.setName(req.name());
        t.setLanguageCode(req.languageCode());
        t.setVersion(1);
        t.setStatus(QuizTemplateStatus.DRAFT);
        t.setScope(scope == null
                ? QuizTemplateScope.SYSTEM
                : QuizTemplateScope.valueOf(scope.trim().toUpperCase()));
        t.setOwnerId(ownerId);
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        t.setQuestions(new ArrayList<>());

        // Duyệt câu hỏi
        int autoNo = 1;
        var used = new java.util.HashSet<Integer>();

        List<com.smokefree.program.web.dto.quiz.template.QuestionUpsertReq> qs =
                (req.questions() == null) ? emptyList()
                        : req.questions();

        for (com.smokefree.program.web.dto.quiz.template.QuestionUpsertReq q : qs) {
            int no = (q.questionNo() != null && q.questionNo() > 0) ? q.questionNo() : autoNo++;
            if (!used.add(no)) {
                throw new com.smokefree.program.web.error.ValidationException("Duplicate questionNo: " + no);
            }

            QuizTemplateQuestion qq = new QuizTemplateQuestion();
            qq.setId(new QuizTemplateQuestionId(null, no)); // templateId sẽ được @MapsId đồng bộ
            qq.setTemplate(t);
            qq.setQuestionText(q.text());
            qq.setType(q.type());          // enum QuestionType
            qq.setPoints(q.points());
            qq.setExplanation(q.explanation());

            List<com.smokefree.program.web.dto.quiz.template.ChoiceUpsertReq> cs =
                    (q.choices() == null) ? emptyList()
                            : q.choices();

            for (com.smokefree.program.web.dto.quiz.template.ChoiceUpsertReq c : cs) {
                QuizChoiceLabel cl = new QuizChoiceLabel();
                cl.setId(new QuizChoiceLabelId(null, null, c.labelCode())); // labelCode đã có
                cl.setLabelText(c.labelText());
                cl.setCorrect(Boolean.TRUE.equals(c.correct()));
                cl.setWeight(c.weight());

                // Gắn về question → setter đồng bộ templateId & questionNo trong PK
                cl.setQuestion(qq);

                qq.getChoiceLabels().add(cl);
            }

            t.getQuestions().add(qq);
        }

        return templateRepo.save(t); // Cascade.ALL sẽ persist toàn bộ graph
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
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));

        QuizTemplateScope scope = src.getScope() == null ? QuizTemplateScope.SYSTEM : src.getScope();
        if (scope != QuizTemplateScope.SYSTEM) {
            throw new ConflictException("Only system templates can be cloned.");
        }

        // Map domain -> DTO (top-level)
        List<QuestionUpsertReq> qReqs = src.getQuestions().stream()
                .sorted(Comparator.comparing(q -> q.getId().getQuestionNo()))
                .map(q -> new QuestionUpsertReq(
                        q.getId().getQuestionNo(),
                        q.getQuestionText(),
                        q.getType(),
                        q.getPoints(),
                        q.getExplanation(),
                        q.getChoiceLabels().stream()
                                .sorted(Comparator.comparing(c -> c.getId().getLabelCode()))
                                .map(c -> new ChoiceUpsertReq(
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

        // Hàm này đã tạo template scope=COACH, owner=coachId
        return createCoachTemplate(req, coachId);
    }

}