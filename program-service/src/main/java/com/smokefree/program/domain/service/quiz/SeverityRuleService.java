
package com.smokefree.program.domain.service.quiz;

import com.smokefree.program.domain.model.SeverityLevel;

public interface SeverityRuleService {

    /**
     * Map tổng điểm quiz -> SeverityLevel
     * Ví dụ quiz 10 câu, thang 1..5 => tổng 10..50.
     * Quy tắc thống nhất:
     *  - <= 20  : LOW
     *  - 21–35 : MODERATE
     *  - >= 36 : HIGH
     */
    SeverityLevel fromScore(int total);

    /**
     * Nhận input chuỗi (tên mức độ hoặc điểm) và trả về SeverityLevel.
     * Hỗ trợ:
     *  - "low", "moderate", "high", "severe", "medium"
     *  - chuỗi số, ví dụ "18", "27", "40" → dùng fromScore(...)
     */

    SeverityLevel fromRaw(String raw);
    int recommendPlanDays(SeverityLevel sev);
    String recommendPlanCode(SeverityLevel sev);
}

