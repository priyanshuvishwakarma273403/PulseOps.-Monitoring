package com.pulseops.alert.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_history")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule alertRule;

    @Column(name = "monitor_id", nullable = false)
    private Long monitorId;

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 50)
    private String channel; // EMAIL, SLACK, TELEGRAM, DISCORD

    @Column(nullable = false, length = 50)
    private String status; // SENT, FAILED

    @Column(name = "fired_at")
    private LocalDateTime firedAt;

    @PrePersist
    protected void onCreate() {
        firedAt = LocalDateTime.now();
    }
}
