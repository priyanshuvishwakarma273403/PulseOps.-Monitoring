package com.pulseops.alert.kafka;

import com.pulseops.alert.entity.*;
import com.pulseops.alert.repository.*;
import com.pulseops.shared.event.AlertEvent;
import com.pulseops.shared.event.HealthCheckEvent;
import com.pulseops.shared.event.IncidentEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class HealthCheckConsumer {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final IncidentRepository incidentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "health-check-events", groupId = "alert-service-group")
    @Transactional
    public void consumeHealthCheck(HealthCheckEvent event) {
        log.info("Received health check event from Kafka for monitor: {} Status: {}", event.getMonitorId(), event.getStatus());

        List<AlertRule> rules = alertRuleRepository.findByMonitorIdAndIsEnabledTrue(event.getMonitorId());
        boolean hasFailures = "DOWN".equalsIgnoreCase(event.getStatus()) || "DEGRADED".equalsIgnoreCase(event.getStatus());

        Optional<Incident> activeIncidentOpt = incidentRepository.findByMonitorIdAndStatus(event.getMonitorId(), "OPEN");

        if (hasFailures) {
            Incident incident;
            if (activeIncidentOpt.isEmpty()) {
                // Create new incident
                incident = Incident.builder()
                        .organizationId(event.getOrganizationId())
                        .monitorId(event.getMonitorId())
                        .title("Incident OPEN: Monitor " + event.getName() + " is reporting " + event.getStatus())
                        .description("Service: " + event.getUrl() + "\nStatus Code: " + event.getStatusCode() + "\nLatency: " + event.getResponseTimeMs() + "ms\nError: " + event.getErrorMessage())
                        .severity("CRITICAL")
                        .status("OPEN")
                        .build();
                incident = incidentRepository.save(incident);
                log.info("Created new Incident: {}", incident.getId());

                // Publish Incident event
                publishIncidentEvent(incident);
            } else {
                incident = activeIncidentOpt.get();
            }

            // Evaluate Alert Rules
            for (AlertRule rule : rules) {
                boolean triggerAlert = false;
                if ("STATUS_DOWN".equalsIgnoreCase(rule.getMetricType()) && "DOWN".equalsIgnoreCase(event.getStatus())) {
                    triggerAlert = true;
                } else if ("LATENCY".equalsIgnoreCase(rule.getMetricType()) && event.getResponseTimeMs() > rule.getThreshold()) {
                    triggerAlert = true;
                }

                if (triggerAlert) {
                    triggerAlert(rule, event, incident.getId());
                }
            }
        } else {
            // Monitor is UP
            if (activeIncidentOpt.isPresent()) {
                Incident incident = activeIncidentOpt.get();
                incident.setStatus("RESOLVED");
                incident.setResolvedAt(LocalDateTime.now());
                incidentRepository.save(incident);
                log.info("Resolved Incident: {}", incident.getId());

                publishIncidentEvent(incident);

                // Publish a resolution alert for rules
                for (AlertRule rule : rules) {
                    sendResolutionAlert(rule, event, incident.getId());
                }
            }
        }
    }

    private void triggerAlert(AlertRule rule, HealthCheckEvent event, Long incidentId) {
        String msg = String.format("ALERT FIRED: Monitor '%s' threshold exceeded. Metric: %s. Value: %s (Threshold: %s). Incident ID: #%d",
                event.getName(), rule.getMetricType(),
                "LATENCY".equalsIgnoreCase(rule.getMetricType()) ? event.getResponseTimeMs() + "ms" : event.getStatus(),
                rule.getThreshold(), incidentId);

        List<String> channels = parseChannels(rule.getChannels());
        for (String channel : channels) {
            AlertHistory history = AlertHistory.builder()
                    .organizationId(rule.getOrganizationId())
                    .alertRule(rule)
                    .monitorId(rule.getMonitorId())
                    .incidentId(incidentId)
                    .message(msg)
                    .channel(channel)
                    .status("SENT")
                    .build();
            alertHistoryRepository.save(history);
        }

        // Publish Alert Event to Kafka
        AlertEvent alertEvent = AlertEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .alertRuleId(rule.getId())
                .organizationId(rule.getOrganizationId())
                .monitorId(rule.getMonitorId())
                .incidentId(incidentId)
                .metricType(rule.getMetricType())
                .message(msg)
                .channels(channels)
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send("alert-events", String.valueOf(rule.getId()), alertEvent);
    }

    private void sendResolutionAlert(AlertRule rule, HealthCheckEvent event, Long incidentId) {
        String msg = String.format("ALERT RESOLVED: Monitor '%s' has recovered. Status is UP. Incident ID: #%d", event.getName(), incidentId);

        List<String> channels = parseChannels(rule.getChannels());
        for (String channel : channels) {
            AlertHistory history = AlertHistory.builder()
                    .organizationId(rule.getOrganizationId())
                    .alertRule(rule)
                    .monitorId(rule.getMonitorId())
                    .incidentId(incidentId)
                    .message(msg)
                    .channel(channel)
                    .status("SENT")
                    .build();
            alertHistoryRepository.save(history);
        }

        AlertEvent alertEvent = AlertEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .alertRuleId(rule.getId())
                .organizationId(rule.getOrganizationId())
                .monitorId(rule.getMonitorId())
                .incidentId(incidentId)
                .metricType(rule.getMetricType())
                .message(msg)
                .channels(channels)
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send("alert-events", String.valueOf(rule.getId()), alertEvent);
    }

    private void publishIncidentEvent(Incident incident) {
        IncidentEvent incidentEvent = IncidentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .incidentId(incident.getId())
                .organizationId(incident.getOrganizationId())
                .monitorId(incident.getMonitorId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send("incident-events", String.valueOf(incident.getId()), incidentEvent);
    }

    private List<String> parseChannels(String channelsJson) {
        if (channelsJson == null || channelsJson.isBlank()) {
            return Collections.singletonList("email");
        }
        try {
            return objectMapper.readValue(channelsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.singletonList("email");
        }
    }
}
