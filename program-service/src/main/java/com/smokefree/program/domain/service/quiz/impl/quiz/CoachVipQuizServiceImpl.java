package com.smokefree.program.domain.service.quiz.impl.quiz;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.*;
import com.smokefree.program.domain.service.quiz.CoachVipQuizService;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.quiz.coach.CloneForVipRes;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CoachVipQuizServiceImpl implements CoachVipQuizService {

    private final QuizTemplateRepository tplRepo;
    private final QuizTemplateQuestionRepository qRepo;
    private final QuizChoiceLabelRepository cRepo;
    private final QuizAssignmentRepository assignRepo;

    @Override
    public CloneForVipRes cloneForUserAndAssign(UUID programId,
                                                UUID userId,
                                                UUID baseTemplateId,
                                                String newName,
                                                OffsetDateTime dueAt,
                                                String note) {

        QuizTemplate base = tplRepo.findById(baseTemplateId)
                .orElseThrow(() -> new IllegalArgumentException("Base template not found"));

        List<QuizTemplateQuestion> baseQs =
                qRepo.findByIdTemplateIdOrderByIdQuestionNoAsc(base.getId());

        // tạo template cá nhân (thuộc coach)
        UUID coachId = SecurityUtil.requireUserId();

        QuizTemplate per = new QuizTemplate();
        per.setId(UUID.randomUUID());
        per.setName((newName == null || newName.isBlank()) ? base.getName() + " (VIP)" : newName);
        per.setStatus(QuizTemplateStatus.DRAFT);
        per.setScope(QuizTemplateScope.COACH);
        per.setOwnerId(coachId);
        per = tplRepo.save(per);

        // clone question + choices
        for (QuizTemplateQuestion q : baseQs) {
            int questionNo = q.getId().getQuestionNo();

            QuizTemplateQuestionId newQId =
                    new QuizTemplateQuestionId(per.getId(), questionNo);

            QuizTemplateQuestion nq = new QuizTemplateQuestion();
            nq.setId(newQId);
            nq.setTemplate(per);
            nq.setQuestionText(q.getQuestionText());
            nq.setType(q.getType());
            nq.setPoints(q.getPoints());
            nq.setExplanation(q.getExplanation());
            qRepo.save(nq);

            // clone choices theo (templateId, questionNo)
            var choices = cRepo.findByIdTemplateIdAndIdQuestionNoOrderByIdLabelCodeAsc(
                    base.getId(), q.getId().getQuestionNo());

            for (QuizChoiceLabel c : choices) {
                String labelCode = c.getId().getLabelCode();

                QuizChoiceLabelId newCId =
                        new QuizChoiceLabelId(per.getId(), questionNo, labelCode);

                QuizChoiceLabel nc = new QuizChoiceLabel();
                nc.setId(newCId);
                nc.setQuestion(nq);
                nc.setLabelText(c.getLabelText());
                nc.setCorrect(c.isCorrect());
                nc.setWeight(c.getWeight());
                cRepo.save(nc);
            }
        }

        // publish ngay để phát bài
        per.setStatus(QuizTemplateStatus.PUBLISHED);
        tplRepo.save(per);

        // tạo assignment cho user VIP
        QuizAssignment a = new QuizAssignment();
        a.setId(UUID.randomUUID());
        a.setTemplateId(per.getId());
        a.setProgramId(programId);

// nguồn phát sinh: coach tuỳ biến
        a.setOrigin(QuizAssignmentOrigin.COACH_CUSTOM);

// phạm vi/chu kỳ: để DAY theo entity (mặc định cũng là DAY)
        a.setScope(AssignmentScope.DAY);

// bài VIP một lần nên không lặp lại => everyDays = null
        a.setEveryDays(null);

// hạn nộp dùng expiresAt thay cho dueAt cũ
        OffsetDateTime deadline = (dueAt != null)
                ? dueAt
                : OffsetDateTime.now(ZoneOffset.UTC).plusDays(7);
        a.setExpiresAt(deadline);

// metadata tạo
        a.setCreatedAt(Instant.now());
        a.setCreatedBy(SecurityUtil.requireUserId());

        assignRepo.save(a);

// trả về thông tin cho client
        return new CloneForVipRes(per.getId(), a.getId(), a.getExpiresAt());
    }
}
