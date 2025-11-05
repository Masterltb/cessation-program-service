package com.smokefree.program.web.dto.me;

import com.smokefree.program.web.dto.enrollment.EnrollmentRes;
import com.smokefree.program.web.dto.subscription.SubscriptionStatusRes;

import java.util.List;

public record DashboardRes(
        SubscriptionStatusRes subscription,
        List<String> dueQuizzes, // ids/codes quiz đến hạn (có thể thay bằng DTO có sẵn của bạn)
        EnrollmentRes activeEnrollment
) {}
