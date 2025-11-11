package com.smokefree.program.domain.service.quiz.impl.quiz;

import com.smokefree.program.domain.model.AssignmentScope;
import com.smokefree.program.domain.model.QuizAssignment;
import com.smokefree.program.domain.model.QuizAssignmentOrigin;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.repo.QuizAssignmentRepository;
import com.smokefree.program.domain.service.quiz.QuizAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizAssignmentServiceImpl implements QuizAssignmentService {

    private static final UUID SYSTEM_USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final QuizAssignmentRepository assignmentRepo;
    private final ProgramRepository programRepo;

    @Override
    @Transactional
    public void assignToPrograms(UUID templateId,
                                 List<UUID> programIds,
                                 int everyDays,
                                 UUID actorId,
                                 String scope) {
        if (programIds == null || programIds.isEmpty()) return;

        // "coach" ở tham số scope đang dùng như quyền tác giả
        boolean authorIsCoach = "coach".equalsIgnoreCase(scope);

        // scope lưu xuống DB là enum AssignmentScope (DAY/WEEK/PROGRAM/CUSTOM)
        AssignmentScope assignmentScope = parseAssignmentScope(scope);

        UUID createdBy = (actorId != null) ? actorId : SYSTEM_USER_ID;
        Instant now = Instant.now();

        List<QuizAssignment> batch = new ArrayList<>(programIds.size());
        for (UUID pid : programIds) {
            // xác minh quyền coach
            if (authorIsCoach && !programRepo.existsByIdAndCoachId(pid, createdBy)) {
                continue;
            }
            // tránh trùng
            if (assignmentRepo.existsByTemplateIdAndProgramId(templateId, pid)) {
                continue;
            }

            QuizAssignment a = new QuizAssignment();
            a.setId(UUID.randomUUID());
            a.setTemplateId(templateId);
            a.setProgramId(pid);
            a.setEveryDays(everyDays);
            a.setScope(assignmentScope);                 // enum OK
            a.setOrigin(QuizAssignmentOrigin.MANUAL);    // ★ bắt buộc để tránh NULL
            a.setCreatedAt(now);
            a.setCreatedBy(createdBy);

            batch.add(a);
        }

        if (!batch.isEmpty()) {
            assignmentRepo.saveAll(batch);
        }
    }

    private AssignmentScope parseAssignmentScope(String s) {
        try {
            return (s == null) ? AssignmentScope.DAY
                    : AssignmentScope.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ignore) {
            return AssignmentScope.DAY;
        }
    }

    @Override
    public List<QuizAssignment> listAssignmentsByProgram(UUID programId) {
        return assignmentRepo.findByProgramId(programId);
    }
}
