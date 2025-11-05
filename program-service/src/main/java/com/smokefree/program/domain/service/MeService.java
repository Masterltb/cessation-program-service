package com.smokefree.program.domain.service;

import com.smokefree.program.web.dto.me.DashboardRes;

import java.util.UUID;

public interface MeService {
    DashboardRes dashboard(UUID userId);
}
