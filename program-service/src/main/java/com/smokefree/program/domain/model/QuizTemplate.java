package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Thực thể đại diện cho một Mẫu câu hỏi (Quiz Template).
 * Chứa thông tin chung, trạng thái và danh sách các câu hỏi.
 */
@Entity
@Table(
        name = "quiz_templates",
        schema = "program",
        indexes = {
                @Index(name = "idx_quiz_template_scope_owner", columnList = "scope, owner_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_quiz_template_name_scope_owner_version",
                columnNames = {"name","scope","owner_id","version"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizTemplate {

    /**
     * Định danh duy nhất (UUID) của mẫu câu hỏi.
     */
    @Id
    private UUID id;

    /**
     * Mã định danh duy nhất (business key) cho mẫu câu hỏi.
     */
    @Column(name = "code", unique = true) // Thêm cột code, đảm bảo là duy nhất
    private String code;

    /**
     * Tên hiển thị của mẫu câu hỏi.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Phiên bản của mẫu câu hỏi (mặc định là 1).
     */
    // DB: DEFAULT 1 NOT NULL
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * Trạng thái hiện tại (DRAFT, PUBLISHED, ARCHIVED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QuizTemplateStatus status;

    /**
     * Mã ngôn ngữ (ví dụ: 'vi', 'en').
     */
    @Column(name = "language_code")
    private String languageCode;

    /**
     * Phạm vi áp dụng (SYSTEM hoặc COACH). Hiện tại chỉ hỗ trợ SYSTEM.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QuizTemplateScope scope;     // SYSTEM | COACH (MVP luôn SYSTEM)

    /**
     * ID người sở hữu (nếu có). Hiện tại luôn là null (hệ thống).
     */
    @Column(name = "owner_id")
    private UUID ownerId;                // luôn null trong MVP (template hệ thống)

    /**
     * Thời điểm xuất bản.
     */
    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Thời điểm lưu trữ.
     */
    @Column(name = "archived_at")
    private Instant archivedAt;

    /**
     * Thời điểm tạo bản ghi.
     */
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /**
     * Thời điểm cập nhật lần cuối.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Danh sách các câu hỏi thuộc mẫu này.
     */
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.questionNo ASC")
    private List<QuizTemplateQuestion> questions = new ArrayList<>();

    /**
     * Xử lý trước khi lưu lần đầu (Persist).
     * Gán ID, ngày tạo, và các giá trị mặc định.
     */
    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        // version mặc định 1
        if (version == null) {
            version = 1;
        }
        // trạng thái mặc định DRAFT
        if (status == null) {
            status = QuizTemplateStatus.DRAFT;
        }
        // ngôn ngữ mặc định 'vi'
        if (languageCode == null || languageCode.isBlank()) {
            languageCode = "vi";
        }

        // MVP: mọi template là SYSTEM, ownerId = null
        scope   = QuizTemplateScope.SYSTEM;
        ownerId = null;

        // name bắt buộc
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("QuizTemplate.name must not be empty");
        }
    }

    /**
     * Xử lý trước khi cập nhật (Update).
     * Cập nhật ngày sửa đổi và đảm bảo các ràng buộc hệ thống.
     */
    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();

        // Giữ ràng buộc SYSTEM-only trong mọi lần update
        scope   = QuizTemplateScope.SYSTEM;
        ownerId = null;
    }
}
