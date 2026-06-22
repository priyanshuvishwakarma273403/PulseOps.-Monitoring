package com.pulseops.alert.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alert_rules")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "monitor_id", nullable = false)
    private Long monitorId;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType; // LATENCY, ERROR_RATE, AVAILABILITY, STATUS_DOWN

    @Column(nullable = false, length = 10)
    private String operator; // >, <, =

    @Column(nullable = false)
    private Double threshold;

    @Column(name = "duration_seconds")
    @Builder.Default
    private Integer durationSeconds = 60;

    @Column(length = 255)
    private String channels; // JSON array of strings e.g. ["email", "slack"]

    @Column(name = "is_enabled")
    @Builder.Default
    private boolean isEnabled = true;
}
