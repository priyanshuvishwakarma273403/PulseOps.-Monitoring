package com.pulseops.alert.repository;

import com.pulseops.alert.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findByMonitorIdAndIsEnabledTrue(Long monitorId);
    List<AlertRule> findByOrganizationId(Long organizationId);
}
