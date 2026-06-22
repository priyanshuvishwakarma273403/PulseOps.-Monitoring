package com.pulseops.alert.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "monitor_id", nullable = false)
    private Long monitorId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(nullable = false, length = 50)
    private String status; // OPEN, INVESTIGATING, RESOLVED, CLOSED

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        status = "OPEN";
    }
}
