// src/main/java/com/smokefree/program/domain/service/quiz/impl/SeverityRuleServiceImpl.java
package com.smokefree.program.domain.service.quiz.impl;

import com.smokefree.program.domain.model.SeverityLevel;
import com.smokefree.program.domain.service.quiz.SeverityRuleService;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class SeverityRuleServiceImpl implements SeverityRuleService {
    @Override
    // Quiz baseline: 10 câu, 1–5 → tổng 10..50
    public SeverityLevel fromScore(int total) {
        if (total <= 20) return SeverityLevel.LOW;
        if (total <= 35) return SeverityLevel.MODERATE;
        return SeverityLevel.HIGH;
    }
    @Override
    // Input có thể là "LOW" / "MODERATE" / "HIGH" hoặc "18", "32" ...
    public SeverityLevel fromRaw(String raw) {
        if (raw == null || raw.isBlank()) return SeverityLevel.LOW;
        String s = raw.trim().toUpperCase();

        // Ưu tiên parse chữ
        if (s.startsWith("SEV"))   return SeverityLevel.HIGH;
        if (s.startsWith("MOD"))   return SeverityLevel.MODERATE;
        if (s.startsWith("LOW"))   return SeverityLevel.LOW;

        // Sau đó parse số
        try {
            int score = Integer.parseInt(s);
            return fromScore(score);
        } catch (NumberFormatException ignore) {
            return SeverityLevel.LOW;
        }
    }
    @Override
    public int recommendPlanDays(SeverityLevel sev) {
        return switch (sev) {
            case LOW      -> 30;
            case MODERATE -> 45;
            case HIGH     -> 60;
        };
    }
    @Override
    // Mapping sang code template seed sẵn
    public String recommendPlanCode(SeverityLevel sev) {
        return switch (sev) {
            case LOW      -> "L1_30D";
            case MODERATE -> "L2_45D";
            case HIGH     -> "L3_60D";
        };
    }

}
