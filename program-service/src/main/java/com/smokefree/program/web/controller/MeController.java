package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.MeService;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.me.DashboardRes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;

    @GetMapping
    public DashboardRes me() {
        return meService.dashboard(SecurityUtil.currentUserId());
    }
}
