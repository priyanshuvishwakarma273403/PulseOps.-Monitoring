package com.pulseops.monitoring.service;

import com.pulseops.monitoring.entity.*;
import com.pulseops.monitoring.repository.*;
import com.pulseops.shared.event.HealthCheckEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckExecutorService {

    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository monitorCheckRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Async("taskExecutor")
    @Transactional
    public void executeCheckAsync(Monitor monitor) {
        log.info("Starting health check for monitor: {} ({})", monitor.getName(), monitor.getUrl());

        long startTime = System.currentTimeMillis();
        int statusCode = -1;
        String status = "UP";
        String errorMessage = null;
        long responseTimeMs = 0;

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(monitor.getUrl()))
                    .timeout(Duration.ofSeconds(10));

            // Set HTTP Method and Body
            if ("GET".equalsIgnoreCase(monitor.getMethod())) {
                requestBuilder.GET();
            } else if ("POST".equalsIgnoreCase(monitor.getMethod())) {
                String body = monitor.getRequestBody() != null ? monitor.getRequestBody() : "";
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
            } else if ("PUT".equalsIgnoreCase(monitor.getMethod())) {
                String body = monitor.getRequestBody() != null ? monitor.getRequestBody() : "";
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
            } else if ("DELETE".equalsIgnoreCase(monitor.getMethod())) {
                requestBuilder.DELETE();
            }

            // Set Headers
            if (monitor.getHeaders() != null && !monitor.getHeaders().isBlank()) {
                try {
                    Map<String, String> headerMap = objectMapper.readValue(monitor.getHeaders(),
                            new TypeReference<Map<String, String>>() {});
                    headerMap.forEach(requestBuilder::header);
                } catch (Exception e) {
                    log.warn("Failed to parse headers for monitor: {}", monitor.getId(), e);
                }
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            statusCode = response.statusCode();
            responseTimeMs = System.currentTimeMillis() - startTime;

            // Evaluate check status
            if (statusCode != monitor.getExpectedStatusCode()) {
                status = "DOWN";
                errorMessage = "Unexpected status code: " + statusCode + " (Expected: " + monitor.getExpectedStatusCode() + ")";
            } else if (responseTimeMs > monitor.getExpectedResponseTime()) {
                status = "DEGRADED";
                errorMessage = "Response time exceeded limit: " + responseTimeMs + "ms (Expected: " + monitor.getExpectedResponseTime() + "ms)";
            }

        } catch (Exception e) {
            status = "DOWN";
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            responseTimeMs = System.currentTimeMillis() - startTime;
        }

        // 1. Save Monitor Check record
        MonitorCheck check = MonitorCheck.builder()
                .monitor(monitor)
                .status(status)
                .statusCode(statusCode == -1 ? null : statusCode)
                .responseTime((int) responseTimeMs)
                .errorMessage(errorMessage)
                .build();
        monitorCheckRepository.save(check);

        // 2. Update Monitor's overall status if changed
        if (!status.equals(monitor.getHealthStatus())) {
            monitor.setHealthStatus(status);
            monitorRepository.save(monitor);
        }

        // 3. Publish check result to Kafka
        HealthCheckEvent event = HealthCheckEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .monitorId(monitor.getId())
                .organizationId(monitor.getOrganizationId())
                .name(monitor.getName())
                .type(monitor.getType())
                .url(monitor.getUrl())
                .status(status)
                .statusCode(statusCode == -1 ? null : statusCode)
                .responseTimeMs(responseTimeMs)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            kafkaTemplate.send("health-check-events", String.valueOf(monitor.getId()), event);
            log.info("Published health-check-event to Kafka for monitor: {}", monitor.getId());
        } catch (Exception e) {
            log.error("Failed to publish health-check-event to Kafka", e);
        }
    }
}
