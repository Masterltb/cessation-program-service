package com.smokefree.program.domain.service;

import com.smokefree.program.web.dto.me.DashboardRes;
import com.smokefree.program.web.dto.subscription.SubscriptionStatusRes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MeServiceImpl implements MeService {
    @Override
    public DashboardRes dashboard(UUID userId) {
        // TODO: gọi các service thật sự (SubscriptionService, MeQuizService, EnrollmentService)
        return new DashboardRes(
                new SubscriptionStatusRes("BASIC", null, new String[]{}),
                List.of(), // due quizzes
                null       // active enrollment
        );
    }
}
