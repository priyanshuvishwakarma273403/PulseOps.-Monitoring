package com.pulseops.alert.repository;

import com.pulseops.alert.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByOrganizationId(Long organizationId);
    Optional<Incident> findByMonitorIdAndStatus(Long monitorId, String status);
    List<Incident> findByOrganizationIdAndStatus(Long organizationId, String status);
}
