package com.pulseops.alert.repository;

import com.pulseops.alert.entity.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {
    List<AlertHistory> findByOrganizationIdOrderByFiredAtDesc(Long organizationId);
}
