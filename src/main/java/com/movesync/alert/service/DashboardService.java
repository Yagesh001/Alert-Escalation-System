package com.movesync.alert.service;

import com.movesync.alert.domain.enums.AlertSeverity;
import com.movesync.alert.domain.enums.AlertStatus;
import com.movesync.alert.domain.model.Alert;
import com.movesync.alert.domain.model.AlertHistory;
import com.movesync.alert.dto.DashboardResponse;
import com.movesync.alert.dto.TopDriverResponse;
import com.movesync.alert.dto.AlertResponse;
import com.movesync.alert.dto.TrendDataResponse;
import com.movesync.alert.repository.AlertHistoryRepository;
import com.movesync.alert.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for dashboard analytics and aggregations
 * Provides real-time visibility into alert trends
 * 
 * Caching Strategy: Dashboard data cached for 5 minutes to reduce DB load
 * Time Complexity: O(n) for aggregations where n = number of alerts
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AlertRepository alertRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    /**
     * Get comprehensive dashboard data
     * Cached for performance
     */
    @Cacheable(value = "dashboard", key = "'overview'")
    public DashboardResponse getDashboardOverview() {
        log.debug("Generating dashboard overview");

        List<AlertStatus> activeStatuses = List.of(AlertStatus.OPEN, AlertStatus.ESCALATED);

        // Get severity counts
        Map<AlertSeverity, Long> severityCounts = getSeverityCounts(activeStatuses);

        // Get top offenders
        List<TopDriverResponse> topDrivers = getTopDrivers(5, activeStatuses);

        // Get recently auto-closed alerts
        List<AlertResponse> recentlyAutoClosedAlerts = getRecentlyAutoClosedAlerts(24);

        // Get recent alert events
        List<AlertHistory> recentEvents = alertHistoryRepository.findTop100ByOrderByTimestampDesc();

        // Get total counts
        long totalOpen = alertRepository.countByStatus(AlertStatus.OPEN);
        long totalEscalated = alertRepository.countByStatus(AlertStatus.ESCALATED);
        long totalAutoClosed = alertRepository.countByStatus(AlertStatus.AUTO_CLOSED);
        long totalResolved = alertRepository.countByStatus(AlertStatus.RESOLVED);

        return DashboardResponse.builder()
            .severityCounts(severityCounts)
            .topDrivers(topDrivers)
            .recentlyAutoClosedAlerts(recentlyAutoClosedAlerts)
            .totalOpenAlerts(totalOpen)
            .totalEscalatedAlerts(totalEscalated)
            .totalAutoClosedAlerts(totalAutoClosed)
            .totalResolvedAlerts(totalResolved)
            .recentEvents(recentEvents)
            .generatedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Get alert counts by severity
     */
    @Cacheable(value = "dashboard", key = "'severity'")
    public Map<AlertSeverity, Long> getSeverityCounts(List<AlertStatus> statuses) {
        log.debug("Getting severity counts");

        List<Object[]> results = alertRepository.countBySeverityAndStatus(statuses);
        Map<AlertSeverity, Long> counts = new HashMap<>();

        // Initialize all severities with 0
        for (AlertSeverity severity : AlertSeverity.values()) {
            counts.put(severity, 0L);
        }

        // Populate with actual counts
        for (Object[] result : results) {
            AlertSeverity severity = (AlertSeverity) result[0];
            Long count = ((Number) result[1]).longValue();
            counts.put(severity, count);
        }

        return counts;
    }

    /**
     * Get top N drivers with most alerts
     * 
     * @param limit Number of top drivers to return
     * @param statuses Alert statuses to consider
     */
    @Cacheable(value = "dashboard", key = "'topDrivers_' + #limit")
    public List<TopDriverResponse> getTopDrivers(int limit, List<AlertStatus> statuses) {
        log.debug("Getting top {} drivers", limit);

        List<Object[]> results = alertRepository.findTopDriversByAlertCount(statuses);
        
        return results.stream()
            .limit(limit)
            .map(result -> {
                String driverId = (String) result[0];
                Long alertCount = ((Number) result[1]).longValue();
                
                // Get breakdown by severity for this driver
                List<Alert> driverAlerts = alertRepository.findByDriverIdAndStatusIn(driverId, statuses);
                Map<AlertSeverity, Long> severityBreakdown = driverAlerts.stream()
                    .collect(Collectors.groupingBy(Alert::getSeverity, Collectors.counting()));

                return TopDriverResponse.builder()
                    .driverId(driverId)
                    .totalAlerts(alertCount)
                    .severityBreakdown(severityBreakdown)
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Get recently auto-closed alerts for transparency
     * 
     * @param hours Number of hours to look back
     */
    public List<AlertResponse> getRecentlyAutoClosedAlerts(int hours) {
        log.debug("Getting recently auto-closed alerts from last {} hours", hours);

        LocalDateTime fromTime = LocalDateTime.now().minusHours(hours);
        List<Alert> alerts = alertRepository.findRecentlyAutoClosedAlerts(fromTime);

        return alerts.stream()
            .map(AlertResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get trend data over time
     * 
     * @param days Number of days to analyze
     */
    @Cacheable(value = "dashboard", key = "'trends_' + #days")
    public List<TrendDataResponse> getTrendData(int days) {
        log.debug("Getting trend data for last {} days", days);

        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        List<AlertHistory> historyEvents = alertHistoryRepository.findRecentEvents(fromDate);

        // Group by date and event type
        Map<String, Map<String, Long>> trendsByDate = historyEvents.stream()
            .collect(Collectors.groupingBy(
                event -> event.getTimestamp().toLocalDate().toString(),
                Collectors.groupingBy(
                    AlertHistory::getEventType,
                    Collectors.counting()
                )
            ));

        return trendsByDate.entrySet().stream()
            .map(entry -> TrendDataResponse.builder()
                .date(entry.getKey())
                .eventCounts(entry.getValue())
                .build())
            .sorted(Comparator.comparing(TrendDataResponse::getDate))
            .collect(Collectors.toList());
    }

    /**
     * Get detailed alert drill-down information
     * Includes history and metadata
     */
    public AlertResponse getAlertDrillDown(String alertId) {
        log.debug("Getting drill-down for alert: {}", alertId);

        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));

        // Get alert history
        List<AlertHistory> history = alertHistoryRepository.findByAlertIdOrderByTimestampAsc(alertId);

        AlertResponse response = AlertResponse.fromEntity(alert);
        response.setHistory(history);

        return response;
    }

    /**
     * Get alert statistics summary
     */
    @Cacheable(value = "dashboard", key = "'stats'")
    public Map<String, Object> getAlertStatistics() {
        log.debug("Generating alert statistics");

        Map<String, Object> stats = new HashMap<>();

        // Count by status
        for (AlertStatus status : AlertStatus.values()) {
            long count = alertRepository.countByStatus(status);
            stats.put(status.name().toLowerCase() + "Count", count);
        }

        // Get active alerts
        List<AlertStatus> activeStatuses = List.of(AlertStatus.OPEN, AlertStatus.ESCALATED);
        long activeCount = alertRepository.findByStatusIn(activeStatuses).size();
        stats.put("activeCount", activeCount);

        // Get severity distribution
        Map<AlertSeverity, Long> severityCounts = getSeverityCounts(activeStatuses);
        stats.put("severityDistribution", severityCounts);

        // Get recent activity
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<Object[]> recentEventCounts = alertHistoryRepository.countEventsByType(last24Hours);
        Map<String, Long> eventCounts = new HashMap<>();
        for (Object[] result : recentEventCounts) {
            eventCounts.put((String) result[0], ((Number) result[1]).longValue());
        }
        stats.put("last24HoursActivity", eventCounts);

        stats.put("generatedAt", LocalDateTime.now());

        return stats;
    }
}

