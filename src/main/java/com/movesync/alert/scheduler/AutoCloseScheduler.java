package com.movesync.alert.scheduler;

import com.movesync.alert.domain.enums.AlertStatus;
import com.movesync.alert.domain.model.Alert;
import com.movesync.alert.monitoring.AlertMetricsService;
import com.movesync.alert.repository.AlertRepository;
import com.movesync.alert.service.RuleEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background scheduler for auto-closing alerts
 * Runs periodically to evaluate and close eligible alerts
 * 
 * Design Principles:
 * - Idempotent: Safe to run multiple times
 * - Fault-tolerant: Continues on individual failures
 * - Batched: Processes alerts in configurable batches
 * 
 * Concurrency Control: Uses AtomicBoolean to prevent overlapping executions
 * Time Complexity: O(n) where n = number of eligible alerts
 * Space Complexity: O(b) where b = batch size
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "alert.auto-close.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class AutoCloseScheduler {

    private final AlertRepository alertRepository;
    private final RuleEvaluationService ruleEvaluationService;
    private final AlertMetricsService metricsService;
    
    @Value("${alert.auto-close.batch-size:100}")
    private int batchSize;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Scheduled job to evaluate and auto-close alerts
     * Runs based on cron expression from configuration
     * Default: Every 5 minutes
     * 
     * Idempotent: Safe to re-run without side effects
     */
    @Scheduled(cron = "${alert.auto-close.cron:0 */5 * * * *}")
    public void evaluateAndAutoCloseAlerts() {
        // Prevent overlapping executions
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Auto-close job already running, skipping this execution");
            return;
        }

        try {
            log.info("Starting auto-close evaluation job");
            long startTime = System.currentTimeMillis();

            // Find active alerts (OPEN or ESCALATED)
            List<AlertStatus> activeStatuses = List.of(AlertStatus.OPEN, AlertStatus.ESCALATED);
            List<Alert> activeAlerts = alertRepository.findByStatusIn(activeStatuses);

            log.info("Found {} active alerts to evaluate", activeAlerts.size());

            if (activeAlerts.isEmpty()) {
                log.debug("No active alerts to process");
                return;
            }

            // Process in batches to control memory usage
            int totalProcessed = 0;
            int totalClosed = 0;

            for (int i = 0; i < activeAlerts.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, activeAlerts.size());
                List<Alert> batch = activeAlerts.subList(i, endIndex);

                log.debug("Processing batch {}-{} of {}", i + 1, endIndex, activeAlerts.size());

                try {
                    int closedInBatch = ruleEvaluationService.batchEvaluateAutoClose(batch);
                    totalClosed += closedInBatch;
                    totalProcessed += batch.size();
                } catch (Exception e) {
                    log.error("Error processing batch {}-{}", i + 1, endIndex, e);
                    // Continue with next batch
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Auto-close job completed: processed={}, closed={}, duration={}ms",
                    totalProcessed, totalClosed, duration);

            // Record metrics
            recordMetrics(totalProcessed, totalClosed, duration);

        } catch (Exception e) {
            log.error("Error in auto-close scheduler", e);
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * Record metrics for monitoring
     * Integrated with Micrometer/Prometheus
     */
    private void recordMetrics(int processed, int closed, long durationMs) {
        // Record custom metrics
        metricsService.recordCustomMetric("auto_close_job.processed", processed);
        metricsService.recordCustomMetric("auto_close_job.closed", closed);
        metricsService.recordCustomMetric("auto_close_job.duration_ms", durationMs);
        
        log.debug("Metrics recorded: alerts_processed={}, alerts_closed={}, job_duration_ms={}",
                 processed, closed, durationMs);
    }

    /**
     * Manual trigger for testing/admin purposes
     * Can be exposed via admin endpoint if needed
     */
    public void triggerManually() {
        log.info("Manual trigger of auto-close job");
        evaluateAndAutoCloseAlerts();
    }

    /**
     * Check if job is currently running
     */
    public boolean isRunning() {
        return isRunning.get();
    }
}

