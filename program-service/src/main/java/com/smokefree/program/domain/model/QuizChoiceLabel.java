package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// QuizChoiceLabel.java
@Entity
@Table(name = "quiz_choice_labels", schema = "program")
@Getter @Setter
public class QuizChoiceLabel {

    @EmbeddedId
    private QuizChoiceLabelId id;

    // Map FK (template_id, question_no) từ id.questionId
    @MapsId("questionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "template_id", referencedColumnName = "template_id"),
            @JoinColumn(name = "question_no", referencedColumnName = "question_no")
    })
    private QuizTemplateQuestion question;

    @Column(name = "label")
    private String label;
}

