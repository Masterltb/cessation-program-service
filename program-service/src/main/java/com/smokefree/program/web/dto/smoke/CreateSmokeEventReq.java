package com.smokefree.program.web.dto.smoke;

import com.smokefree.program.domain.model.SmokeEventKind;
import com.smokefree.program.domain.model.SmokeEventType;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * DTO yêu cầu tạo mới một sự kiện liên quan đến việc hút thuốc (Smoke) hoặc cơn thèm thuốc (Urge).
 * <p>
 * Dùng để ghi nhận các hành vi của người dùng trong quá trình cai thuốc,
 * giúp hệ thống phân tích và đưa ra các biện pháp can thiệp phù hợp.
 * </p>
 */
public record CreateSmokeEventReq(
        /** Loại sự kiện (ví dụ: SMOKE - hút thuốc, URGE - cơn thèm). Bắt buộc. */
        @NotNull SmokeEventType eventType, // SMOKE/URGE...

        /** Ghi chú thêm của người dùng về sự kiện này (tùy chọn). */
        String note,

        /** Thời điểm sự kiện được ghi nhận trên hệ thống (thường là thời điểm gọi API). */
        OffsetDateTime eventAt,

        /** Thời điểm thực tế sự kiện xảy ra (do người dùng chọn, có thể trong quá khứ). */
        OffsetDateTime occurredAt,

        /** Phân loại chi tiết (ví dụ: SLIP - lỡ hút, RELAPSE - tái nghiện, hoặc mức độ cơn thèm). Bắt buộc. */
        @NotNull SmokeEventKind kind,

        /** Số hơi thuốc đã hút (nếu là sự kiện hút thuốc). */
        Integer puffs, // Thêm trường puffs

        /** Lý do dẫn đến việc hút thuốc hoặc cơn thèm (ví dụ: Stress, Vui vẻ, Thói quen). */
        String reason   // Thêm trường reason
        ) {}
