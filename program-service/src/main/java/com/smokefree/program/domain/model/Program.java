package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "programs", schema = "program")
public class Program {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable=false)
    private UUID userId;
    public UUID coachId;

    private UUID chatroomId;

    @Column(nullable=false)
    private int planDays; // 30|45|60

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private ProgramStatus status = ProgramStatus.ACTIVE;

    @Column(nullable=false)
    private LocalDate startDate;
    @Column(nullable=false)
    private int currentDay = 1;

    private Integer totalScore;
    @Enumerated(EnumType.STRING)
    private SeverityLevel severity;

    // snapshot trial
    private String entitlementTierAtCreation;
    private Instant trialStartedAt;
    private Instant trialEndExpected;

    @Column(nullable=false)
    private Instant createdAt;

    @Column(nullable=false)
    private Instant updatedAt;

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

    @Column(name = "streak_current", nullable = false)
    private int streakCurrent = 0;

    @Column(name = "streak_best", nullable = false)
    private int streakBest = 0;

    @Column(name = "last_smoke_at")
    // optional: columnDefinition = "timestamptz"
    private OffsetDateTime lastSmokeAt;

    // Thêm nếu DB có cột này
    @Column(name = "streak_frozen_until")
    private OffsetDateTime streakFrozenUntil;
}
