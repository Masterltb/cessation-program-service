package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "quiz_template_questions", schema = "program")
@Getter @Setter
public class QuizTemplateQuestion {

    @EmbeddedId
    private QuizTemplateQuestionId id;

    @MapsId("templateId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private QuizTemplate template;

    // Nội dung & thuộc tính nghiệp vụ
    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "type", nullable = false, columnDefinition = "program.question_type")
    private QuestionType type;

    @Column(name = "points")
    private Integer points;

    @Column(name = "explanation")
    private String explanation;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // child
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.labelCode ASC")                 // <— dùng id.labelCode, KHÔNG phải labelCode
    private Set<QuizChoiceLabel> choiceLabels = new LinkedHashSet<>();

    // tiện ích
    @Transient
    public Integer getOrderNo() { return id != null ? id.getQuestionNo() : null; }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
