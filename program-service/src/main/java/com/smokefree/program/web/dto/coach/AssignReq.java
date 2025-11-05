package com.smokefree.program.web.dto.coach;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignReq(@NotNull UUID coachId, @NotNull UUID customerId) {}
