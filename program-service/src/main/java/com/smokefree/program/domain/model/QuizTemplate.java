package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// QuizTemplate.java  (đã có – bổ sung thuộc tính)
// QuizTemplate.java
// com.smokefree.program.domain.model.QuizTemplate
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
public class QuizTemplate {

    @Id
    private UUID id;

    private String name;
    private Integer version;

    @Enumerated(EnumType.STRING)
    private QuizTemplateStatus status; // DRAFT/PUBLISHED/ARCHIVED

    private String languageCode;

    // NEW -----
    @Column(length = 20, nullable = false)
    private String scope;   // "system" | "coach"

    @Column(name = "owner_id")
    private UUID ownerId;   // null với system, có giá trị với coach
    // ---------

    private Instant publishedAt;
    private Instant archivedAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.questionNo ASC")
    private List<QuizTemplateQuestion> questions = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now; updatedAt = now;
        if (status == null) status = QuizTemplateStatus.DRAFT;
        if (scope == null) scope = "system";
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}



