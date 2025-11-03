package com.smokefree.program.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

// QuizTemplateQuestionId.java
@Embeddable
@Getter @Setter @EqualsAndHashCode
public class QuizTemplateQuestionId implements Serializable {
    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "question_no")
    private Integer questionNo;
}
