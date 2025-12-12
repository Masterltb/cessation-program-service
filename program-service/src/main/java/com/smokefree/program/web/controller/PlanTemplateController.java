// src/main/java/com/smokefree/program/web/controller/PlanTemplateController.java
package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.template.PlanTemplateService;
import com.smokefree.program.web.dto.plan.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý các mẫu lộ trình cai thuốc (Plan Templates).
 * Cung cấp API để lấy danh sách, chi tiết và nội dung từng ngày của lộ trình.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plan-templates")
public class PlanTemplateController {

    private final PlanTemplateService service;

    /**
     * Lấy danh sách tóm tắt tất cả các mẫu lộ trình có sẵn.
     *
     * @return Danh sách PlanTemplateSummaryRes.
     */
    @GetMapping
    public List<PlanTemplateSummaryRes> list() {
        return service.listAll();
    }

    /**
     * Lấy thông tin chi tiết của một mẫu lộ trình cụ thể.
     *
     * @param id ID của mẫu lộ trình.
     * @return Chi tiết mẫu lộ trình.
     */
    @GetMapping("/{id}")
    public PlanTemplateDetailRes detail(@PathVariable UUID id) {
        return service.getDetail(id);
    }

    /**
     * Lấy danh sách các ngày và nhiệm vụ trong lộ trình dựa trên mã code.
     *
     * @param code   Mã code của lộ trình (ví dụ: L1_30D).
     * @param expand Có mở rộng chi tiết nhiệm vụ hay không.
     * @param lang   Ngôn ngữ (mặc định là 'vi').
     * @return Cấu trúc các ngày trong lộ trình.
     */
    @GetMapping("/by-code/{code}/days")
    public PlanDaysRes daysByCode(@PathVariable String code,
                                  @RequestParam(defaultValue = "false") boolean expand,
                                  @RequestParam(defaultValue = "vi") String lang) {
        return service.getDaysByCode(code, expand, lang);
    }

    /**
     * Lấy danh sách các ngày và nhiệm vụ trong lộ trình dựa trên ID.
     *
     * @param id     ID của mẫu lộ trình.
     * @param expand Có mở rộng chi tiết nhiệm vụ hay không.
     * @param lang   Ngôn ngữ (mặc định là 'vi').
     * @return Cấu trúc các ngày trong lộ trình.
     */
    @GetMapping("/{id}/days")
    public PlanDaysRes days(@PathVariable UUID id,
                            @RequestParam(defaultValue = "false") boolean expand,
                            @RequestParam(defaultValue = "vi") String lang) {
        return service.getDays(id, expand, lang);
    }

    /**
     * Đề xuất lộ trình phù hợp dựa trên mức độ nghiêm trọng của việc hút thuốc.
     *
     * @param severity Mức độ nghiêm trọng (LOW, MODERATE, HIGH).
     * @return Lộ trình được đề xuất.
     */
    @GetMapping("/recommendation")
    public PlanRecommendationRes recommend(@RequestParam String severity) {
        return service.recommendBySeverity(severity);
    }
}