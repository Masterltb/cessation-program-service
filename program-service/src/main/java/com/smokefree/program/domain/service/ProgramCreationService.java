// src/main/java/com/smokefree/program/domain/service/ProgramCreationService.java
package com.smokefree.program.domain.service;

import com.smokefree.program.domain.model.Program;

import java.util.UUID;

public interface ProgramCreationService {

    Program createTrialProgram(UUID userId,
                               int planDays,
                               int trialDays,
                               String tierHeader);

    Program createPaidProgram(UUID userId,
                              int planDays,
                              String tierHeader);
}
