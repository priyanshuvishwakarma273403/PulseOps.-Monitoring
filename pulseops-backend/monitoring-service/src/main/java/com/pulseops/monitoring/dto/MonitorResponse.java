package com.pulseops.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonitorResponse {
    private Long id;
    private Long organizationId;
    private String name;
    private String type;
    private String url;
    private String method;
    private String headers;
    private String requestBody;
    private Integer expectedStatusCode;
    private Integer expectedResponseTime;
    private Integer checkInterval;
    private String status;
    private String healthStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
