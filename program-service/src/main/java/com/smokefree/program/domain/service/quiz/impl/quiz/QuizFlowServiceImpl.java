package com.smokefree.program.domain.service.quiz.impl.quiz;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.*;
import com.smokefree.program.domain.service.quiz.QuizFlowService;
import com.smokefree.program.web.dto.quiz.answer.AnswerReq;
import com.smokefree.program.web.dto.quiz.attempt.DueItem;
import com.smokefree.program.web.dto.quiz.attempt.OpenAttemptRes;
import com.smokefree.program.web.dto.quiz.result.SubmitRes;
import com.smokefree.program.web.error.ConflictException;
import com.smokefree.program.web.error.ForbiddenException;
import com.smokefree.program.web.error.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizFlowServiceImpl implements QuizFlowService {

    private final QuizAssignmentRepository assignmentRepo;
    private final QuizTemplateRepository templateRepo;
    private final QuizAttemptRepository attemptRepo;
    private final QuizResultRepository resultRepo;
    private final ProgramRepository programRepo;

    @Override
    public List<DueItem> listDue(UUID userId) {
        Program program = programRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Program not found for user"));

        List<QuizAssignment> assigns = assignmentRepo.findByProgramId(program.getId());
        Instant now = Instant.now();
        List<DueItem> out = new ArrayList<>();

        for (var a : assigns) {
            Instant last = resultRepo
                    .findFirstByProgramIdAndTemplateIdOrderByCreatedAtDesc(program.getId(), a.getTemplateId())
                    .map(QuizResult::getCreatedAt)
                    // nếu chưa có result nào, lấy ngày bắt đầu program
                    .orElse(program.getStartDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC));

            int gap = (a.getEveryDays() == null || a.getEveryDays() <= 0) ? 5 : a.getEveryDays();
            Instant due = last.plus(Duration.ofDays(gap));
            if (!due.isAfter(now)) {
                QuizTemplate t = templateRepo.findById(a.getTemplateId()).orElseThrow();
                out.add(new DueItem(t.getId(), program.getId(), t.getName(), due));
            }
        }
        return out;
    }

    @Override
    @Transactional
    public OpenAttemptRes openAttempt(UUID userId, UUID templateId) {
        Program program = programRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Program not found"));

        if (!assignmentRepo.existsByTemplateIdAndProgramId(templateId, program.getId())) {
            throw new ForbiddenException("Template not assigned to your program");
        }

        attemptRepo.findFirstByProgramIdAndTemplateIdAndStatus(
                program.getId(), templateId, AttemptStatus.OPEN
        ).ifPresent(a -> { throw new ConflictException("An attempt is already open"); });

        QuizTemplate t = templateRepo.findById(templateId).orElseThrow();

        QuizAttempt at = new QuizAttempt();
        at.setId(UUID.randomUUID());
        at.setProgramId(program.getId());
        at.setTemplateId(t.getId());
        at.setUserId(userId);
        at.setOpenedAt(Instant.now());
        at.setStatus(AttemptStatus.OPEN);

        // ✅ Đúng kiểu:
        at.setAnswers(new ArrayList<QuizAnswer>());  // hoặc bỏ dòng này nếu entity đã có = new ArrayList<>()
        attemptRepo.save(at);

        List<OpenAttemptRes.QuestionView> qs = t.getQuestions().stream()
                .map(q -> new OpenAttemptRes.QuestionView(
                        q.getId().getQuestionNo(),
                        q.getText(),
                        q.getChoiceLabels().stream()
                                .sorted(Comparator.comparing(c -> c.getId().getScore()))
                                .collect(Collectors.toMap(
                                        c -> c.getId().getScore(),
                                        QuizChoiceLabel::getLabel,
                                        (a, b) -> a,
                                        LinkedHashMap::new
                                ))
                ))
                .toList();

        return new OpenAttemptRes(at.getId(), t.getId(), t.getVersion(), qs);
    }



    @Override
    public void saveAnswer(UUID userId, UUID templateId, UUID attemptId, AnswerReq req) {
        QuizAttempt at = attemptRepo.findById(attemptId).orElseThrow(() -> new NotFoundException("Attempt not found"));
        if (!at.getUserId().equals(userId) || !at.getTemplateId().equals(templateId) || at.getStatus() != AttemptStatus.OPEN) {
            throw new ForbiddenException("Invalid attempt");
        }

        QuizAnswerId id = new QuizAnswerId();
        id.setAttemptId(attemptId);
        id.setQuestionNo(req.questionNo());

        // upsert
        at.getAnswers().removeIf(a -> a.getId().getQuestionNo().equals(req.questionNo()));
        QuizAnswer ans = new QuizAnswer();
        ans.setId(id);
        ans.setAttempt(at);
        ans.setAnswer(req.answer());
        ans.setCreatedAt(Instant.now());
        at.getAnswers().add(ans);

        attemptRepo.save(at);
    }

    @Override
    public SubmitRes submit(UUID userId, UUID templateId, UUID attemptId) {
        QuizAttempt at = attemptRepo.findById(attemptId).orElseThrow(() -> new NotFoundException("Attempt not found"));
        if (!at.getUserId().equals(userId) || !at.getTemplateId().equals(templateId) || at.getStatus() != AttemptStatus.OPEN) {
            throw new ForbiddenException("Invalid attempt");
        }

        int total = at.getAnswers().stream().mapToInt(QuizAnswer::getAnswer).sum();
        SeverityLevel sev = toSeverity(total);

        QuizTemplate t = templateRepo.findById(templateId).orElseThrow();

        QuizResult r = new QuizResult();
        r.setId(UUID.randomUUID());
        r.setProgramId(at.getProgramId());
        r.setTemplateId(templateId);
        r.setQuizVersion(t.getVersion());
        r.setTotalScore(total);
        r.setSeverity(sev);
        r.setCreatedAt(Instant.now());
        resultRepo.save(r);

        at.setStatus(AttemptStatus.SUBMITTED);
        at.setSubmittedAt(Instant.now());
        attemptRepo.save(at);

        return new SubmitRes(at.getId(), total, sev.name());
    }

    private SeverityLevel toSeverity(int total) {
        // Chuẩn điểm ví dụ (10 câu, thang 1-5) => 10..50
        if (total <= 15) return SeverityLevel.LOW;
        if (total <= 25) return SeverityLevel.MODERATE; // hoặc MODERATE tùy enum của bạn
        return SeverityLevel.HIGH;
    }
}
