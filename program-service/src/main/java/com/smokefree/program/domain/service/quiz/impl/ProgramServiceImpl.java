package com.smokefree.program.domain.service.quiz.impl;

import com.smokefree.program.domain.model.ProgramStatus;
import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.service.ProgramCreationService;
import com.smokefree.program.domain.service.ProgramService;
import com.smokefree.program.web.dto.program.*;
import com.smokefree.program.web.error.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepository repo;
    private final ProgramCreationService programCreationService;
    @Override
    @Transactional
    public ProgramRes createProgram(UUID ownerUserId, CreateProgramReq req, String tierHeader) {
        repo.findFirstByUserIdAndStatusAndDeletedAtIsNull(ownerUserId, ProgramStatus.ACTIVE)
                .ifPresent(p -> { throw new ConflictException("User already has ACTIVE program"); });

        int planDays = (req.planDays() == null ? 30 : req.planDays());

        // Paid program: không trial
        Program p = programCreationService.createPaidProgram(ownerUserId, planDays, tierHeader);
        p = repo.save(p);

        return toRes(p, "ACTIVE", null, tierHeader);
    }

    @Override
    public Optional<Program> getActive(UUID userId) {
        return repo.findFirstByUserIdAndStatusAndDeletedAtIsNull(userId, ProgramStatus.ACTIVE);
    }

    @Override
    public ProgramRes toRes(Program p, String entState, Instant entExp, String tier) {
        String effectiveTier = (tier == null) ? "basic" : tier;

        List<String> features;
        if ("vip".equalsIgnoreCase(effectiveTier)) {
            features = List.of("forum", "coach-1-1");
        } else if ("premium".equalsIgnoreCase(effectiveTier)) {
            features = List.of("forum");
        } else {
            features = Collections.emptyList(); // đúng kiểu List<String>
        }

        Entitlements ent = new Entitlements(effectiveTier, features);
        Access access = new Access(entState, entExp, tier);

        return new ProgramRes(
                p.getId(), p.getStatus(), p.getPlanDays(), p.getStartDate(),
                p.getCurrentDay(), p.getSeverity(), p.getTotalScore(), ent, access
        );
    }
    @Override
    public List<Program> listByUser(UUID userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
