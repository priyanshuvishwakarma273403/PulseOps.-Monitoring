package com.pulseops.monitoring.repository;

import com.pulseops.monitoring.entity.MonitorCheck;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MonitorCheckRepository extends JpaRepository<MonitorCheck, Long> {
    List<MonitorCheck> findByMonitorIdOrderByCheckedAtDesc(Long monitorId, Pageable pageable);

    @Query("SELECT mc FROM MonitorCheck mc WHERE mc.monitor.id = :monitorId AND mc.checkedAt >= CURRENT_TIMESTAMP - 1 DAY ORDER BY mc.checkedAt ASC")
    List<MonitorCheck> findRecentChecksForMonitor(Long monitorId);
}
