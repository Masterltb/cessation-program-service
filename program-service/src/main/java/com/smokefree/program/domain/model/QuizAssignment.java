package com.smokefree.program.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="quiz_assignments", schema="program")
@Getter @Setter
public class QuizAssignment {
    @Id private UUID id;

    @Column(name="template_id") private UUID templateId;
    @Column(name="program_id")  private UUID programId;
    @Column(name="every_days")  private Integer everyDays;  // KHỚP tên cột
    @Column(name="created_at", updatable=false) private Instant createdAt;
    @Column(name="created_by") private UUID createdBy;     // KHỚP tên cột
    @Column(name="scope", length=16) private String scope; // hoặc enum như trên
}

