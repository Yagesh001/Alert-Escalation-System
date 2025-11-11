package com.movesync.alert.dto;

import com.movesync.alert.domain.enums.AlertSeverity;
import com.movesync.alert.domain.model.AlertHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for dashboard overview response
 * Aggregates all key metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    // Severity breakdown
    private Map<AlertSeverity, Long> severityCounts;

    // Top offenders
    private List<TopDriverResponse> topDrivers;

    // Recent auto-closed alerts
    private List<AlertResponse> recentlyAutoClosedAlerts;

    // Total counts
    private Long totalOpenAlerts;
    private Long totalEscalatedAlerts;
    private Long totalAutoClosedAlerts;
    private Long totalResolvedAlerts;

    // Recent activity stream
    private List<AlertHistory> recentEvents;

    // Metadata
    private LocalDateTime generatedAt;
}

