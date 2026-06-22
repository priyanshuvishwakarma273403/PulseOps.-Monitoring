package com.pulseops.alert.controller;

import com.pulseops.alert.entity.AlertHistory;
import com.pulseops.alert.entity.AlertRule;
import com.pulseops.alert.repository.AlertHistoryRepository;
import com.pulseops.alert.repository.AlertRuleRepository;
import com.pulseops.shared.dto.ApiResponse;
import com.pulseops.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<AlertRule>> createRule(
            @RequestBody AlertRule rule,
            @RequestHeader("X-Org-Id") Long organizationId) {
        rule.setOrganizationId(organizationId);
        AlertRule saved = alertRuleRepository.save(rule);
        return ResponseEntity.ok(ApiResponse.success(saved, "Alert rule created successfully"));
    }

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<AlertRule>>> listRules(
            @RequestHeader("X-Org-Id") Long organizationId) {
        List<AlertRule> rules = alertRuleRepository.findByOrganizationId(organizationId);
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AlertHistory>>> getHistory(
            @RequestHeader("X-Org-Id") Long organizationId) {
        List<AlertHistory> history = alertHistoryRepository.findByOrganizationIdOrderByFiredAtDesc(organizationId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
