package com.pulseops.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HealthCheckEvent {
    private String eventId;
    private Long monitorId;
    private Long organizationId;
    private String name;
    private String type;
    private String url;
    private String status; // UP, DOWN, DEGRADED
    private Integer statusCode;
    private Long responseTimeMs;
    private String errorMessage;
    private LocalDateTime timestamp;
}
