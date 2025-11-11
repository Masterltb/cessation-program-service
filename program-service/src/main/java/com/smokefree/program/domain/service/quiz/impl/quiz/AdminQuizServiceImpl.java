package com.smokefree.program.domain.service.quiz.impl.quiz;


import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.QuizChoiceLabelRepository;
import com.smokefree.program.domain.repo.QuizTemplateQuestionRepository;
import com.smokefree.program.domain.repo.QuizTemplateRepository;
import com.smokefree.program.domain.service.quiz.AdminQuizService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminQuizServiceImpl implements AdminQuizService {

    private final QuizTemplateRepository tplRepo;
    private final QuizTemplateQuestionRepository qRepo;
    private final QuizChoiceLabelRepository cRepo;

    @Override
    public QuizTemplate createTemplate(String name) {
        QuizTemplate t = new QuizTemplate();
        t.setId(UUID.randomUUID());
        t.setName(name);
        t.setVersion(1);
        t.setStatus(QuizTemplateStatus.DRAFT);
        t.setScope(QuizTemplateScope.SYSTEM);
        t.setOwnerId(null);
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        return tplRepo.save(t);
    }

    @Override
    public void publishTemplate(UUID templateId) {
        QuizTemplate t = tplRepo.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        t.setStatus(QuizTemplateStatus.PUBLISHED);
        t.setPublishedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        tplRepo.save(t);
    }

    @Override
    public QuizTemplateQuestionId addQuestion(
            UUID templateId, Integer orderNo, String text, String type, Integer points, String explanation) {

        QuizTemplate template = tplRepo.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        QuizTemplateQuestionId id = new QuizTemplateQuestionId(templateId, orderNo);

        QuizTemplateQuestion q = new QuizTemplateQuestion();
        q.setId(id);
        q.setTemplate(template);
        q.setQuestionText(text);
        q.setType(QuestionType.valueOf(type));   // truyền trực tiếp QuestionType để tránh sai chính tả
        q.setPoints(points);
        q.setExplanation(explanation);

        qRepo.save(q);
        return id;
    }

    @Override
    public QuizChoiceLabelId addChoice(
            UUID templateId, Integer questionNo,
            String labelCode, String labelText,
            Boolean correct, Integer weight) {

        // Bảo đảm câu hỏi tồn tại để liên kết ManyToOne
        QuizTemplateQuestionId qid = new QuizTemplateQuestionId(templateId, questionNo);
        QuizTemplateQuestion question = qRepo.findById(qid)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        QuizChoiceLabelId cid = new QuizChoiceLabelId(templateId, questionNo, labelCode);

        QuizChoiceLabel c = new QuizChoiceLabel();
        c.setId(cid);
        c.setQuestion(question);         // đồng bộ templateId/questionNo trong PK
        c.setLabelText(labelText);
        c.setCorrect(Boolean.TRUE.equals(correct));
        c.setWeight(weight);

        cRepo.save(c);
        return cid;
    }
}
