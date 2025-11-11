package com.smokefree.program.web.controller.quiz;

import com.smokefree.program.domain.service.quiz.CoachVipQuizService;

import com.smokefree.program.web.dto.quiz.coach.CloneForVipReq;
import com.smokefree.program.web.dto.quiz.coach.CloneForVipRes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/programs/{programId}/coach/quiz")
@RequiredArgsConstructor
public class CoachVipQuizController {

    private final CoachVipQuizService service;

    /**
     * Coach clone template gốc thành template cá nhân cho user VIP và phát bài ngay.
     * Chặn quyền:
     * - Coach của program, và user mục tiêu là VIP trong program (đã kiểm tra qua Header/X-Claims).
     */
    @PostMapping("/templates/{templateId}/clone-for/{userId}")
    @PreAuthorize("@authz.isCoach(#programId) && @authz.isVip(#programId)")
    public CloneForVipRes cloneForVip(@PathVariable UUID programId,
                                      @PathVariable UUID templateId,
                                      @PathVariable UUID userId,
                                      @RequestBody(required=false) CloneForVipReq req) {
        return service.cloneForUserAndAssign(
                programId, userId, templateId,
                req == null ? null : req.newName(),
                req == null ? null : req.expiresAt(),
                req == null ? null : req.note()
        );
    }
}
