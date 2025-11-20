// src/main/java/com/smokefree/program/domain/service/impl/ProgramCreationServiceImpl.java
package com.smokefree.program.domain.service;

import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.ProgramStatus;
import com.smokefree.program.domain.service.ProgramCreationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class ProgramCreationServiceImpl implements ProgramCreationService {

    @Override
    public Program createTrialProgram(UUID userId,
                                      int planDays,
                                      int trialDays,
                                      String tierHeader) {

        Instant now = Instant.now();

        return Program.builder()
                .userId(userId)
                .planDays(planDays)
                .status(ProgramStatus.ACTIVE)
                .startDate(LocalDate.now(ZoneOffset.UTC))
                .currentDay(1)
                .entitlementTierAtCreation(tierHeader)
                .trialStartedAt(now)
                .trialEndExpected(now.plus(trialDays, ChronoUnit.DAYS))
                .build();
    }

    @Override
    public Program createPaidProgram(UUID userId,
                                     int planDays,
                                     String tierHeader) {

        return Program.builder()
                .userId(userId)
                .planDays(planDays)
                .status(ProgramStatus.ACTIVE)
                .startDate(LocalDate.now(ZoneOffset.UTC))
                .currentDay(1)
                .entitlementTierAtCreation(tierHeader)
                .trialStartedAt(null)
                .trialEndExpected(null)
                .build();
    }
}
