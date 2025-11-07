package com.smokefree.program.domain.service.smoke.impl;
import com.smokefree.program.domain.model.SmokeEvent;
import com.smokefree.program.domain.model.SmokeEventType;
import com.smokefree.program.domain.repo.SmokeEventRepository;

import com.smokefree.program.domain.service.smoke.SmokeEventService;
import com.smokefree.program.domain.service.smoke.StreakService;
import com.smokefree.program.web.dto.smoke.CreateSmokeEventReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmokeEventServiceImpl implements SmokeEventService {

    private final SmokeEventRepository smokeRepo;
    private final StreakService streakService;

    @Override
    @Transactional
    public SmokeEvent create(UUID programId, CreateSmokeEventReq req) {
        UUID current = com.smokefree.program.util.SecurityUtil.currentUserId();
        if (current == null) {
            throw new IllegalStateException("Missing authenticated user");
        }
        SmokeEvent e = new SmokeEvent();
        e.setId(UUID.randomUUID());
        e.setProgramId(programId);
        e.setEventType(req.eventType());
        e.setUserId(current);
        e.setEventAt(req.eventAt() != null ? req.eventAt() : OffsetDateTime.now(ZoneOffset.UTC));
        e.setNote(req.note());
        smokeRepo.save(e);

        if (e.getEventType() == SmokeEventType.SMOKE) {
            streakService.breakStreak(programId, e.getEventAt(), e.getId(), e.getNote());
        }
        return e;
    }
}

