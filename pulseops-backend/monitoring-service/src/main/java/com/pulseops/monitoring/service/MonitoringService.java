package com.pulseops.monitoring.service;

import com.pulseops.monitoring.dto.*;
import com.pulseops.monitoring.entity.*;
import com.pulseops.monitoring.repository.*;
import com.pulseops.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository monitorCheckRepository;

    @Transactional
    public MonitorResponse createMonitor(MonitorRequest request, Long organizationId) {
        Monitor monitor = Monitor.builder()
                .organizationId(organizationId)
                .name(request.getName())
                .type(request.getType())
                .url(request.getUrl())
                .method(request.getMethod())
                .headers(request.getHeaders())
                .requestBody(request.getRequestBody())
                .expectedStatusCode(request.getExpectedStatusCode())
                .expectedResponseTime(request.getExpectedResponseTime())
                .checkInterval(request.getCheckInterval())
                .status("ACTIVE")
                .healthStatus("UP")
                .build();

        Monitor saved = monitorRepository.save(monitor);
        return mapToResponse(saved);
    }

    @Transactional
    public MonitorResponse updateMonitor(Long id, MonitorRequest request, Long organizationId) {
        Monitor monitor = monitorRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));

        monitor.setName(request.getName());
        monitor.setType(request.getType());
        monitor.setUrl(request.getUrl());
        monitor.setMethod(request.getMethod());
        monitor.setHeaders(request.getHeaders());
        monitor.setRequestBody(request.getRequestBody());
        monitor.setExpectedStatusCode(request.getExpectedStatusCode());
        monitor.setExpectedResponseTime(request.getExpectedResponseTime());
        monitor.setCheckInterval(request.getCheckInterval());

        Monitor saved = monitorRepository.save(monitor);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteMonitor(Long id, Long organizationId) {
        Monitor monitor = monitorRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));
        monitor.setDeleted(true);
        monitorRepository.save(monitor);
    }

    public MonitorResponse getMonitorById(Long id, Long organizationId) {
        Monitor monitor = monitorRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));
        return mapToResponse(monitor);
    }

    public List<MonitorResponse> listMonitors(Long organizationId) {
        return monitorRepository.findByOrganizationIdAndIsDeletedFalse(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MonitorCheckDto> getChecksForMonitor(Long id, Long organizationId) {
        // Verify ownership
        monitorRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));

        return monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(id, PageRequest.of(0, 100)).stream()
                .map(check -> MonitorCheckDto.builder()
                        .id(check.getId())
                        .status(check.getStatus())
                        .responseTime(check.getResponseTime())
                        .statusCode(check.getStatusCode())
                        .errorMessage(check.getErrorMessage())
                        .checkedAt(check.getCheckedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public MonitorResponse pauseMonitor(Long id, Long organizationId) {
        Monitor monitor = monitorRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));
        monitor.setStatus("PAUSED");
        return mapToResponse(monitorRepository.save(monitor));
    }

    @Transactional
    public MonitorResponse resumeMonitor(Long id, Long organizationId) {
        Monitor monitor = monitorRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));
        monitor.setStatus("ACTIVE");
        return mapToResponse(monitorRepository.save(monitor));
    }

    public MonitorResponse mapToResponse(Monitor m) {
        return MonitorResponse.builder()
                .id(m.getId())
                .organizationId(m.getOrganizationId())
                .name(m.getName())
                .type(m.getType())
                .url(m.getUrl())
                .method(m.getMethod())
                .headers(m.getHeaders())
                .requestBody(m.getRequestBody())
                .expectedStatusCode(m.getExpectedStatusCode())
                .expectedResponseTime(m.getExpectedResponseTime())
                .checkInterval(m.getCheckInterval())
                .status(m.getStatus())
                .healthStatus(m.getHealthStatus())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
