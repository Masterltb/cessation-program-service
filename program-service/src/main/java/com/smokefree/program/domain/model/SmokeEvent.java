package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "smoke_events", schema = "program")
@Getter @Setter
public class SmokeEvent {
    @Id private UUID id;

    @Column(name="program_id", nullable=false)
    private UUID programId;

    @Column(name = "event_at")
    private OffsetDateTime eventAt;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(name="event_type", nullable=false, columnDefinition = "program.smoke_event_type")
    private SmokeEventType eventType = SmokeEventType.SMOKE;

    @Column(name = "user_id", nullable = false)     // PHẢI có và KHÔNG set insertable=false
    private UUID userId;

    @Column(name="note")
    private String note;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @PrePersist void pre() {
        if (createdAt == null) createdAt = Instant.now();
        if (eventAt == null) eventAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
