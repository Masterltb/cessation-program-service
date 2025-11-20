package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "smoke_events", schema = "program")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmokeEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, columnDefinition = "program.smoke_event_kind")
    private SmokeEventKind kind;

    @Column(name = "puffs")
    private Integer puffs;

    @Column(name = "cigarettes")
    private Integer cigarettes;

    @Column(name = "reason")
    private String reason;

    @Column(name = "repair_action")
    private String repairAction;

    @Column(name = "repaired")
    private Boolean repaired;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, columnDefinition = "program.smoke_event_type")
    private SmokeEventType eventType;

    @Column(name = "event_at", nullable = false)
    private OffsetDateTime eventAt;

    @Column(name = "note")
    private String note;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        OffsetDateTime nowOffset = OffsetDateTime.now(ZoneOffset.UTC);
        if (eventAt == null) eventAt = nowOffset;
        if (occurredAt == null) occurredAt = eventAt;
    }
}
