package com.pulseops.monitoring.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MonitorRequest {
    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^(REST|GRAPHQL|WEBSOCKET|SSL|DNS)$", message = "Type must be REST, GRAPHQL, WEBSOCKET, SSL, or DNS")
    private String type;

    @NotBlank
    private String url;

    @NotBlank
    @Pattern(regexp = "^(GET|POST|PUT|DELETE)$", message = "Method must be GET, POST, PUT, or DELETE")
    private String method;

    private String headers;
    private String requestBody;

    @Min(100)
    @Max(599)
    private Integer expectedStatusCode = 200;

    @Min(50)
    private Integer expectedResponseTime = 1000; // ms

    @Min(10)
    @Max(3600)
    private Integer checkInterval = 60; // seconds
}
