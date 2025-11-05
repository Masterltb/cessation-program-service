package com.smokefree.program.web.dto.coach;

import java.time.Instant;
import java.util.UUID;

public record CoachAssignmentRes(UUID id, UUID coachId, UUID customerId, Instant createdAt) {}
