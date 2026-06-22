package com.pulseops.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlertEvent {
    private String eventId;
    private Long alertRuleId;
    private Long organizationId;
    private Long monitorId;
    private Long incidentId;
    private String metricType;
    private String message;
    private List<String> channels;
    private LocalDateTime timestamp;
}
