package com.smokefree.program.domain.service.template;


import com.smokefree.program.web.dto.plan.PlanDaysRes;
import com.smokefree.program.web.dto.plan.PlanRecommendationRes;
import com.smokefree.program.web.dto.plan.PlanTemplateDetailRes;
import com.smokefree.program.web.dto.plan.PlanTemplateSummaryRes;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý các mẫu lộ trình cai thuốc (Plan Templates).
 * Cung cấp các chức năng để truy xuất danh sách, chi tiết và đề xuất lộ trình phù hợp.
 */
public interface PlanTemplateService {

    /**
     * Lấy danh sách tóm tắt tất cả các mẫu lộ trình hiện có.
     *
     * @return Danh sách các mẫu lộ trình.
     */
    List<PlanTemplateSummaryRes> listAll();

    /**
     * Lấy thông tin chi tiết của một mẫu lộ trình cụ thể.
     *
     * @param id ID của mẫu lộ trình.
     * @return Chi tiết mẫu lộ trình.
     */
    PlanTemplateDetailRes getDetail(UUID id);

    /**
     * Đề xuất lộ trình phù hợp dựa trên mức độ nghiêm trọng của việc hút thuốc.
     *
     * @param severity Mức độ nghiêm trọng (ví dụ: LOW, MODERATE, HIGH).
     * @return Lộ trình được đề xuất.
     */
    PlanRecommendationRes recommendBySeverity(String severity);

    /**
     * Lấy danh sách các ngày và nhiệm vụ chi tiết trong lộ trình theo ID.
     *
     * @param templateId   ID của mẫu lộ trình.
     * @param expandModule Có mở rộng chi tiết nội dung bài học hay không.
     * @param lang         Ngôn ngữ.
     * @return Cấu trúc dữ liệu phân theo ngày.
     */
    PlanDaysRes getDays(UUID templateId, boolean expandModule, String lang);

    /**
     * Lấy danh sách các ngày và nhiệm vụ chi tiết trong lộ trình theo mã code.
     *
     * @param code         Mã code của lộ trình.
     * @param expandModule Có mở rộng chi tiết nội dung bài học hay không.
     * @param lang         Ngôn ngữ.
     * @return Cấu trúc dữ liệu phân theo ngày.
     */
    PlanDaysRes getDaysByCode(String code, boolean expandModule, String lang);
}
