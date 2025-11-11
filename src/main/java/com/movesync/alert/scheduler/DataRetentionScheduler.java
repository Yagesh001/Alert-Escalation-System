package com.movesync.alert.scheduler;

import com.movesync.alert.repository.AlertHistoryRepository;
import com.movesync.alert.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduler for data retention and cleanup
 * Removes old alerts and history to manage storage
 * 
 * Space Optimization: Deletes data older than retention period
 * Runs: Daily at 2:00 AM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataRetentionScheduler {

    private final AlertRepository alertRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    @Value("${alert.retention.days:90}")
    private int retentionDays;

    /**
     * Clean up old alerts and history
     * Runs daily at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldData() {
        log.info("Starting data retention cleanup job");
        
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
            
            log.info("Deleting alerts and history older than {} (retention: {} days)",
                    threshold, retentionDays);

            // Delete old alerts
            alertRepository.deleteOldAlerts(threshold);
            log.info("Old alerts deleted successfully");

            // Delete old history
            alertHistoryRepository.deleteOldHistory(threshold);
            log.info("Old alert history deleted successfully");

            log.info("Data retention cleanup completed");

        } catch (Exception e) {
            log.error("Error during data retention cleanup", e);
        }
    }
}

