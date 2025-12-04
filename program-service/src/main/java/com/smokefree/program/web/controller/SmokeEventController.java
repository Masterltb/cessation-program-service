package com.smokefree.program.web.controller;


import com.smokefree.program.domain.service.smoke.SmokeEventService;
import com.smokefree.program.web.dto.smoke.CreateSmokeEventReq;
import com.smokefree.program.web.dto.smoke.SmokeEventRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/programs/{programId}/smoke-events")
@RequiredArgsConstructor
public class SmokeEventController {
    private final SmokeEventService service;

    @PostMapping

    public SmokeEventRes create(@PathVariable UUID programId,
                                @Valid @RequestBody CreateSmokeEventReq req) {
        var e = service.create(programId, req);
        return SmokeEventRes.from(e);
    }
}
