//package com.smokefree.program.domain.service;
//
//import com.smokefree.program.domain.model.Program;
//import com.smokefree.program.domain.model.ProgramStatus;
//
//import java.time.Instant;
//import java.time.LocalDate;
//import java.time.ZoneOffset;
//import java.time.temporal.ChronoUnit;
//import java.util.UUID;
//
//public final class ProgramFactory {
//
//    private ProgramFactory() {}
//
//    public static Program newTrialProgram(UUID userId,
//                                          int planDays,
//                                          int trialDays,
//                                          String tierHeader) {
//        Instant now = Instant.now();
//
//        return Program.builder()
//                .userId(userId)
//                .planDays(planDays)
//                .status(ProgramStatus.ACTIVE)
//                .startDate(LocalDate.now(ZoneOffset.UTC))
//                .currentDay(1)
//                .entitlementTierAtCreation(tierHeader)
//                .trialStartedAt(now)
//                .trialEndExpected(now.plus(trialDays, ChronoUnit.DAYS))
//                .build();
//    }
//
//    public static Program newPaidProgram(UUID userId,
//                                         int planDays,
//                                         String tierHeader) {
//        return Program.builder()
//                .userId(userId)
//                .planDays(planDays)
//                .status(ProgramStatus.ACTIVE)
//                .startDate(LocalDate.now(ZoneOffset.UTC))
//                .currentDay(1)
//                .entitlementTierAtCreation(tierHeader)
//                .trialStartedAt(null)
//                .trialEndExpected(null)
//                .build();
//    }
//}
