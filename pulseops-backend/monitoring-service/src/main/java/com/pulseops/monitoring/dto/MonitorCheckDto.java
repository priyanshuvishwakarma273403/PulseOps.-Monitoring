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
public class MonitorCheckDto {
    private Long id;
    private String status;
    private Integer responseTime;
    private Integer statusCode;
    private String errorMessage;
    private LocalDateTime checkedAt;
}
