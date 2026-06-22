package com.pulseops.alert.controller;

import com.pulseops.alert.entity.Incident;
import com.pulseops.alert.entity.IncidentComment;
import com.pulseops.alert.repository.IncidentCommentRepository;
import com.pulseops.alert.repository.IncidentRepository;
import com.pulseops.shared.dto.ApiResponse;
import com.pulseops.shared.exception.ResourceNotFoundException;
import com.pulseops.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentRepository incidentRepository;
    private final IncidentCommentRepository incidentCommentRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Incident>>> listIncidents(
            @RequestHeader("X-Org-Id") Long organizationId) {
        List<Incident> incidents = incidentRepository.findByOrganizationId(organizationId);
        return ResponseEntity.ok(ApiResponse.success(incidents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Incident>> getIncident(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long organizationId) {
        Incident incident = incidentRepository.findById(id)
                .filter(i -> i.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
        return ResponseEntity.ok(ApiResponse.success(incident));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Incident>> updateIncidentStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader("X-Org-Id") Long organizationId) {
        Incident incident = incidentRepository.findById(id)
                .filter(i -> i.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
        
        incident.setStatus(status.toUpperCase());
        if ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        Incident saved = incidentRepository.save(incident);
        return ResponseEntity.ok(ApiResponse.success(saved, "Incident status updated successfully"));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<IncidentComment>> addComment(
            @PathVariable Long id,
            @RequestBody IncidentComment commentRequest,
            @RequestHeader("X-Org-Id") Long organizationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        Incident incident = incidentRepository.findById(id)
                .filter(i -> i.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));

        IncidentComment comment = IncidentComment.builder()
                .incident(incident)
                .userId(principal.getId())
                .comment(commentRequest.getComment())
                .build();
        IncidentComment saved = incidentCommentRepository.save(comment);
        return ResponseEntity.ok(ApiResponse.success(saved, "Comment added successfully"));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<IncidentComment>>> getComments(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long organizationId) {
        
        // Verify incident ownership
        incidentRepository.findById(id)
                .filter(i -> i.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));

        List<IncidentComment> comments = incidentCommentRepository.findByIncidentIdOrderByCreatedAtAsc(id);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
}
