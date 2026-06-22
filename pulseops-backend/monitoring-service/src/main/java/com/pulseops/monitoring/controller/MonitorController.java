package com.pulseops.monitoring.controller;

import com.pulseops.monitoring.dto.*;
import com.pulseops.monitoring.service.MonitoringService;
import com.pulseops.shared.dto.ApiResponse;
import com.pulseops.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/monitors")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitoringService monitoringService;

    @PostMapping
    public ResponseEntity<ApiResponse<MonitorResponse>> createMonitor(
            @Valid @RequestBody MonitorRequest request,
            @RequestHeader("X-Org-Id") Long organizationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        MonitorResponse response = monitoringService.createMonitor(request, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Monitor created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MonitorResponse>> updateMonitor(
            @PathVariable Long id,
            @Valid @RequestBody MonitorRequest request,
            @RequestHeader("X-Org-Id") Long organizationId) {
        
        MonitorResponse response = monitoringService.updateMonitor(id, request, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Monitor updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMonitor(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long organizationId) {
        
        monitoringService.deleteMonitor(id, organizationId);
        return ResponseEntity.ok(ApiResponse.success("Monitor deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MonitorResponse>> getMonitor(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long organizationId) {
        
        MonitorResponse response = monitoringService.getMonitorById(id, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MonitorResponse>>> listMonitors(
            @RequestHeader("X-Org-Id") Long organizationId) {
        
        List<MonitorResponse> response = monitoringService.listMonitors(organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/checks")
    public ResponseEntity<ApiResponse<List<MonitorCheckDto>>> getMonitorChecks(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long organizationId) {
        
        List<MonitorCheckDto> response = monitoringService.getChecksForMonitor(id, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<MonitorResponse>> pauseMonitor(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long organizationId) {
        
        MonitorResponse response = monitoringService.pauseMonitor(id, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Monitor check execution paused"));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<MonitorResponse>> resumeMonitor(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long organizationId) {
        
        MonitorResponse response = monitoringService.resumeMonitor(id, organizationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Monitor check execution resumed"));
    }
}
