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
public class IncidentEvent {
    private String eventId;
    private Long incidentId;
    private Long organizationId;
    private Long monitorId;
    private String title;
    private String description;
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    private String status; // OPEN, INVESTIGATING, RESOLVED, CLOSED
    private LocalDateTime timestamp;
}
