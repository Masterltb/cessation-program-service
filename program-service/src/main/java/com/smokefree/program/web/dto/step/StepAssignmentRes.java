package com.smokefree.program.web.dto.step;

import com.smokefree.program.domain.model.StepAssignment;
import com.smokefree.program.domain.model.StepStatus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StepAssignmentRes(
        UUID id,
        UUID programId,
        Integer stepNo,
        Integer plannedDay,
        StepStatus status,
        String note,
        OffsetDateTime scheduledAt,
        OffsetDateTime completedAt,
        Instant createdAt
) {
    public static StepAssignmentRes from(StepAssignment s) {
        return new StepAssignmentRes(
                s.getId(),
                s.getProgramId(),
                s.getStepNo(),
                s.getPlannedDay(),
                s.getStatus(),
                s.getNote(),
                s.getScheduledAt(),
                s.getCompletedAt(),
                s.getCreatedAt()
        );
    }
}


