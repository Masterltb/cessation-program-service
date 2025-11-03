package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @EqualsAndHashCode
public class QuizChoiceLabelId implements Serializable {

    // Khóa câu hỏi (template_id + question_no)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "templateId", column = @Column(name = "template_id")),
            @AttributeOverride(name = "questionNo", column = @Column(name = "question_no"))
    })
    private QuizTemplateQuestionId questionId;

    @Column(name = "score")
    private Integer score;
}

