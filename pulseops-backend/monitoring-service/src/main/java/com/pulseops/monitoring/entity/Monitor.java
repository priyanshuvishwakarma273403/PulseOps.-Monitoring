package com.pulseops.monitoring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "monitors")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type; // REST, GRAPHQL, WEBSOCKET, SSL, DNS

    @Column(nullable = false, length = 512)
    private String url;

    @Column(nullable = false, length = 10)
    private String method; // GET, POST, PUT, DELETE

    @Column(columnDefinition = "TEXT")
    private String headers; // JSON String

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "expected_status_code")
    @Builder.Default
    private Integer expectedStatusCode = 200;

    @Column(name = "expected_response_time")
    @Builder.Default
    private Integer expectedResponseTime = 1000; // ms

    @Column(name = "check_interval")
    @Builder.Default
    private Integer checkInterval = 60; // seconds

    @Column(nullable = false, length = 50)
    private String status; // ACTIVE, PAUSED

    @Column(name = "health_status", nullable = false, length = 50)
    @Builder.Default
    private String healthStatus = "UP"; // UP, DOWN, DEGRADED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private boolean isDeleted;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = "ACTIVE";
        healthStatus = "UP";
        isDeleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
