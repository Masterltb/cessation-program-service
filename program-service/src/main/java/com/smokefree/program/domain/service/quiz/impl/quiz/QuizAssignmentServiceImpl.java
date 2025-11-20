// src/main/java/com/smokefree/program/domain/service/quiz/impl/quiz/QuizAssignmentServiceImpl.java
package com.smokefree.program.domain.service.quiz.impl.quiz;

import com.smokefree.program.domain.model.AssignmentScope;
import com.smokefree.program.domain.model.QuizAssignment;
import com.smokefree.program.domain.model.QuizAssignmentOrigin;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.repo.QuizAssignmentRepository;
import com.smokefree.program.domain.service.quiz.QuizAssignmentService;
import com.smokefree.program.util.SecurityUtil;
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
                                 AssignmentScope scope) {
        if (programIds == null || programIds.isEmpty()) return;

        UUID createdBy = (actorId != null) ? actorId : SYSTEM_USER_ID;
        Instant now = Instant.now();

        boolean isCoach = SecurityUtil.hasRole("COACH") && !SecurityUtil.hasRole("ADMIN");

        List<QuizAssignment> batch = new ArrayList<>(programIds.size());
        for (UUID pid : programIds) {

            // Nếu là COACH (không phải ADMIN) → chỉ assign cho program mình phụ trách
            if (isCoach && !programRepo.existsByIdAndCoachId(pid, createdBy)) {
                continue;
            }

            // Tránh trùng assignment
            if (assignmentRepo.existsByTemplateIdAndProgramId(templateId, pid)) {
                continue;
            }

            QuizAssignment a = new QuizAssignment();
            a.setId(UUID.randomUUID());
            a.setTemplateId(templateId);
            a.setProgramId(pid);
            a.setEveryDays(everyDays);
            a.setScope(scope);                          // DAY/WEEK/PROGRAM/CUSTOM
            a.setOrigin(QuizAssignmentOrigin.MANUAL);   // tránh NULL
            a.setCreatedAt(now);
            a.setCreatedBy(createdBy);

            batch.add(a);
        }

        if (!batch.isEmpty()) {
            assignmentRepo.saveAll(batch);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizAssignment> listAssignmentsByProgram(UUID programId) {
        return assignmentRepo.findByProgramId(programId);
    }
}
