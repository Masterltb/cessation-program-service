package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.smoke.SmokeEventService;
import com.smokefree.program.web.dto.smoke.SmokeEventRes;
import com.smokefree.program.web.dto.smoke.SmokeEventStatisticsRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Quản lý Smoke Events: history, statistics.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/programs/{programId}/smoke-events")
@Slf4j
public class SmokeEventDetailController {

    private final SmokeEventService smokeEventService;

    /**
     * Lịch sử các lần hút.
     */

    public List<SmokeEventRes> getHistory(
            @PathVariable UUID programId,
            @RequestParam(defaultValue = "20") int size) {

        log.info("[SmokeEvent] Get history for program {} size {}", programId, size);
        return smokeEventService.getHistory(programId, size).stream()
                .map(SmokeEventRes::from)
                .toList();
    }

    /**
     * Thống kê theo kỳ.
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public SmokeEventStatisticsRes getStatistics(
            @PathVariable UUID programId,
            @RequestParam(defaultValue = "WEEK") String period) { // DAY, WEEK, MONTH

        log.info("[SmokeEvent] Get statistics for program {} period {}", programId, period);
        return smokeEventService.getStatistics(programId, period);
    }
}
