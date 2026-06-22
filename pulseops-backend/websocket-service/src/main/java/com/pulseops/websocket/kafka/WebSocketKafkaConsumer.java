package com.pulseops.websocket.kafka;

import com.pulseops.shared.event.AlertEvent;
import com.pulseops.shared.event.HealthCheckEvent;
import com.pulseops.shared.event.IncidentEvent;
import com.pulseops.websocket.handler.RealtimeDashboardHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketKafkaConsumer {

    private final RealtimeDashboardHandler dashboardHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "health-check-events", groupId = "websocket-service-group")
    public void consumeHealthCheck(HealthCheckEvent event) {
        log.debug("WebSocket Service received health-check-event from Kafka: {}", event.getMonitorId());
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", "HEALTH_CHECK");
            payload.put("data", event);
            String json = objectMapper.writeValueAsString(payload);
            dashboardHandler.broadcastToOrg(event.getOrganizationId(), json);
        } catch (Exception e) {
            log.error("Failed to broadcast health-check-event to client", e);
        }
    }

    @KafkaListener(topics = "incident-events", groupId = "websocket-service-group")
    public void consumeIncident(IncidentEvent event) {
        log.info("WebSocket Service received incident-event from Kafka: {}", event.getIncidentId());
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", "INCIDENT");
            payload.put("data", event);
            String json = objectMapper.writeValueAsString(payload);
            dashboardHandler.broadcastToOrg(event.getOrganizationId(), json);
        } catch (Exception e) {
            log.error("Failed to broadcast incident-event to client", e);
        }
    }

    @KafkaListener(topics = "alert-events", groupId = "websocket-service-group")
    public void consumeAlert(AlertEvent event) {
        log.info("WebSocket Service received alert-event from Kafka: {}", event.getAlertRuleId());
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", "ALERT");
            payload.put("data", event);
            String json = objectMapper.writeValueAsString(payload);
            dashboardHandler.broadcastToOrg(event.getOrganizationId(), json);
        } catch (Exception e) {
            log.error("Failed to broadcast alert-event to client", e);
        }
    }
}
