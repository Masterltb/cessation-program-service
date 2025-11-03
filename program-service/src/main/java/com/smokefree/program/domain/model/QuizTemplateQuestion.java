package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;


// QuizTemplateQuestion.java
@Entity
@Table(name = "quiz_template_questions", schema = "program")
@Getter @Setter
public class QuizTemplateQuestion {

    @EmbeddedId
    private QuizTemplateQuestionId id;

    @MapsId("templateId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private QuizTemplate template;

    @Column(name = "text")
    private String text;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuizChoiceLabel> choiceLabels = new LinkedHashSet<>();

    @PrePersist
    void prePersist() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}

