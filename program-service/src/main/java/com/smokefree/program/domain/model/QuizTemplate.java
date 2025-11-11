package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizTemplate {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    // version có thể null (để unique still work theo chuẩn Postgres – null != null)
    @Column(name = "version")
    private Integer version;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status",
            nullable = false,
            columnDefinition = "program.quiz_template_status")
    private QuizTemplateStatus status;

    @Column(name = "language_code")
    private String languageCode;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "scope",
            nullable = false,
            columnDefinition = "program.quiz_template_scope")
    private QuizTemplateScope scope;    // SYSTEM | COACH

    @Column(name = "owner_id")
    private UUID ownerId;               // null với system, có giá trị với coach

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Chú ý: mappedBy="template" khi child có @ManyToOne QuizTemplate template
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.questionNo ASC")
    private List<QuizTemplateQuestion> questions = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null)              id = UUID.randomUUID();
        if (createdAt == null)       createdAt = now;
        if (updatedAt == null)       updatedAt = now;

        // mặc định an toàn
        if (status == null)          status = QuizTemplateStatus.DRAFT;

        // chuẩn hoá scope theo ownerId
        if (scope == null) {
            scope = (ownerId == null) ? QuizTemplateScope.SYSTEM : QuizTemplateScope.COACH;
        }

        // name bắt buộc
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("QuizTemplate.name must not be empty");
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        // scope là enum → không cần trim/lowercase
    }
}
