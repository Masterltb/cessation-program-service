// src/main/java/com/smokefree/program/domain/service/quiz/QuizAssignmentService.java
package com.smokefree.program.domain.service.quiz;

import com.smokefree.program.domain.model.AssignmentScope;
import com.smokefree.program.domain.model.QuizAssignment;

import java.util.List;
import java.util.UUID;

public interface QuizAssignmentService {

    void assignToPrograms(UUID templateId,
                          List<UUID> programIds,
                          int everyDays,
                          UUID actorId,
                          AssignmentScope scope);

    List<QuizAssignment> listAssignmentsByProgram(UUID programId);
}
