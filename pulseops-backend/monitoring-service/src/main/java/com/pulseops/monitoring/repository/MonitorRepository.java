package com.pulseops.monitoring.repository;

import com.pulseops.monitoring.entity.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, Long> {
    List<Monitor> findByOrganizationIdAndIsDeletedFalse(Long organizationId);
    List<Monitor> findByStatusAndIsDeletedFalse(String status);
    Optional<Monitor> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long organizationId);
}
