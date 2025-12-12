package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.*;
import java.util.UUID;

/**
 * Entity đại diện cho một chương trình cai thuốc (Program) của người dùng.
 * <p>
 * Đây là aggregate root quản lý trạng thái, tiến độ, điểm số và các thông tin
 * liên quan đến lộ trình cai thuốc cá nhân hóa.
 * </p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "programs", schema = "program")
public class Program {
    /** ID duy nhất của chương trình. */
    @Id @GeneratedValue
    private UUID id;

    /** ID của người dùng sở hữu chương trình này. */
    @Column(nullable=false)
    private UUID userId;

    /** ID của huấn luyện viên (Coach) được gán (nếu có). */
    public UUID coachId;

    /** ID phòng chat hỗ trợ (nếu có). */
    private UUID chatroomId;

    /** Tổng số ngày của lộ trình (ví dụ: 30, 45, 60 ngày). */
    @Column(nullable=false)
    private int planDays; // 30|45|60

    /** Trạng thái hiện tại của chương trình (ACTIVE, COMPLETED, DROPPED...). */
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProgramStatus status = ProgramStatus.ACTIVE;

    /** Ngày bắt đầu thực tế của chương trình. */
    @Column(nullable=false)
    private LocalDate startDate;

    /** Ngày hiện tại trong lộ trình (tăng dần theo thời gian, bắt đầu từ 1). */
    @Column(nullable=false)
    private int currentDay = 1;

    /** Tổng điểm đánh giá từ bài kiểm tra đầu vào (Baseline Quiz). */
    private Integer totalScore;

    /** Mức độ nghiện thuốc (LOW, MODERATE, HIGH) dựa trên Baseline Quiz. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SeverityLevel severity;

    // --- Trial & Entitlement Snapshots ---

    /** Ghi lại hạng thành viên (Tier) tại thời điểm tạo chương trình (ví dụ: "free", "premium"). */
    private String entitlementTierAtCreation;

    /** Thời điểm bắt đầu dùng thử (nếu là chương trình dùng thử). */
    private Instant trialStartedAt;

    /** Thời điểm dự kiến kết thúc dùng thử. Dùng để kiểm tra logic khóa truy cập. */
    private Instant trialEndExpected;

    // --- Audit Fields ---

    @Column(nullable=false)
    private Instant createdAt;

    @Column(nullable=false)
    private Instant updatedAt;

    /** Thời điểm xóa mềm (Soft delete). Nếu null nghĩa là chưa xóa. */
    private Instant deletedAt;

    @PrePersist void preInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (startDate == null) startDate = LocalDate.now(ZoneOffset.UTC);
    }
    @PreUpdate void preUpdate(){
        updatedAt = Instant.now();
    }

    // --- Gamification & Streak Tracking ---

    /** Chuỗi ngày không hút thuốc hiện tại. */
    @Column(name = "streak_current", nullable = false)
    private int streakCurrent = 0;

    /** Kỷ lục chuỗi ngày không hút thuốc tốt nhất trong chương trình này. */
    @Column(name = "streak_best", nullable = false)
    private int streakBest = 0;

    /** Thời điểm lần cuối người dùng báo cáo hút thuốc (Relapse/Slip). */
    @Column(name = "last_smoke_at")
    private OffsetDateTime lastSmokeAt;

    /** Thời điểm mà chuỗi streak bị đóng băng (ví dụ: dùng tính năng "Freeze Streak"). */
    @Column(name = "streak_frozen_until")
    private OffsetDateTime streakFrozenUntil;

    // --- Template Info ---

    /** ID của mẫu lộ trình (PlanTemplate) được sử dụng. */
    @Column(name = "plan_template_id")
    private UUID planTemplateId;

    /** Mã code của template (snapshot, ví dụ: "L1_30D"). */
    @Column(name = "template_code")
    private String templateCode;

    /** Tên hiển thị của template (snapshot). */
    @Column(name = "template_name")
    private String templateName;

    /** Số lần người dùng đã sử dụng tính năng phục hồi chuỗi (Recovery Quiz). */
    @Column(name = "streak_recovery_used_count", nullable = false)
    private int streakRecoveryUsedCount = 0;

    /** Cờ đánh dấu chương trình có đang bị tạm dừng hay không. */
    @Column(name = "has_paused")
    private boolean hasPaused = false;
}
