package com.smokefree.program.domain.service.template.impl;


import com.smokefree.program.domain.service.template.PlanTemplateService;
import com.smokefree.program.web.dto.plan.PlanRecommendationRes;
import com.smokefree.program.web.dto.plan.PlanTemplateDetailRes;
import com.smokefree.program.web.dto.plan.PlanTemplateSummaryRes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PlanTemplateServiceImpl implements PlanTemplateService {

    @Override
    public List<PlanTemplateSummaryRes> listAll() {
        // TODO: thay bằng repo thực
        return List.of(
                new PlanTemplateSummaryRes(
                        UUID.fromString("00000000-0000-0000-0000-000000000030"),
                        "LIGHT_30D", "30-day Light Dependence Plan",
                        "Lộ trình 30 ngày cho mức nhẹ", 30),
                new PlanTemplateSummaryRes(
                        UUID.fromString("00000000-0000-0000-0000-000000000045"),
                        "MOD_45D", "45-day Moderate Plan",
                        "Lộ trình 45 ngày cho mức trung bình", 45),
                new PlanTemplateSummaryRes(
                        UUID.fromString("00000000-0000-0000-0000-000000000060"),
                        "SEV_60D", "60-day Severe Plan",
                        "Lộ trình 60 ngày cho mức nặng", 60)
        );
    }

    @Override
    public PlanTemplateDetailRes getDetail(UUID id) {
        // TODO: thay bằng repo thực; tạm mock từ id → 1 plan
        var steps = List.of(
                new PlanTemplateDetailRes.StepRes(1,"Giới thiệu","Đặt mục tiêu tuần 1",0,"EDU"),
                new PlanTemplateDetailRes.StepRes(2,"Tập thói quen","Bài tập thay thế",3,"TASK"),
                new PlanTemplateDetailRes.StepRes(3,"Đánh giá","Làm quiz tuần 1",7,"QUIZ")
        );
        return new PlanTemplateDetailRes(id,"LIGHT_30D","30-day Light Dependence Plan",
                "Lộ trình 30 ngày cho mức nhẹ",30, steps);
    }

    @Override
    public PlanRecommendationRes recommendBySeverity(String severity) {
        // TODO: map theo tổng điểm quiz của bạn
        String s = severity==null? "LOW" : severity.toUpperCase();
        if (s.startsWith("SEV")) {
            return new PlanRecommendationRes("SEVERE",
                    UUID.fromString("00000000-0000-0000-0000-000000000060"),
                    "SEV_60D","60-day Severe Plan",60,"Điểm cao → cần 60 ngày");
        } else if (s.startsWith("MOD")) {
            return new PlanRecommendationRes("MODERATE",
                    UUID.fromString("00000000-0000-0000-0000-000000000045"),
                    "MOD_45D","45-day Moderate Plan",45,"Điểm trung bình → 45 ngày");
        }
        return new PlanRecommendationRes("LOW",
                UUID.fromString("00000000-0000-0000-0000-000000000030"),
                "LIGHT_30D","30-day Light Dependence Plan",30,"Điểm thấp → 30 ngày");
    }
}

