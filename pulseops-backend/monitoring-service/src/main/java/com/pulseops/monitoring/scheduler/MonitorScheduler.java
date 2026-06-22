package com.pulseops.monitoring.scheduler;

import com.pulseops.monitoring.entity.Monitor;
import com.pulseops.monitoring.repository.MonitorRepository;
import com.pulseops.monitoring.service.CheckExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitorScheduler {

    private final MonitorRepository monitorRepository;
    private final CheckExecutorService checkExecutorService;

    // Stores the last execution timestamp of each monitor in-memory
    private final Map<Long, LocalDateTime> lastCheckMap = new HashMap<>();

    @Scheduled(fixedDelay = 5000)
    public void scheduleChecks() {
        log.info("Scheduling pending active monitors...");
        List<Monitor> activeMonitors = monitorRepository.findByStatusAndIsDeletedFalse("ACTIVE");

        LocalDateTime now = LocalDateTime.now();
        for (Monitor monitor : activeMonitors) {
            LocalDateTime lastCheck = lastCheckMap.get(monitor.getId());
            if (lastCheck == null || lastCheck.plusSeconds(monitor.getCheckInterval()).isBefore(now)) {
                lastCheckMap.put(monitor.getId(), now);
                checkExecutorService.executeCheckAsync(monitor);
            }
        }
    }
}
