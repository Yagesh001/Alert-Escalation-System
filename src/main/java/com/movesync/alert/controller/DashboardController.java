package com.movesync.alert.controller;

import com.movesync.alert.dto.AlertResponse;
import com.movesync.alert.dto.ApiResponse;
import com.movesync.alert.dto.DashboardResponse;
import com.movesync.alert.dto.TrendDataResponse;
import com.movesync.alert.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Dashboard and Analytics
 * Provides aggregated views and statistics
 * 
 * Caching: Dashboard data is cached to reduce database load
 * Cache TTL: 5 minutes (configurable)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Analytics", description = "APIs for dashboard visualizations and analytics")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get comprehensive dashboard overview
     * GET /api/v1/dashboard/overview
     * 
     * Returns:
     * - Severity counts (Critical, Warning, Info)
     * - Top 5 drivers with most alerts
     * - Recent auto-closed alerts
     * - Total counts by status
     * - Recent activity stream
     * 
     * @return Dashboard overview data
     */
    @GetMapping("/overview")
    @Operation(summary = "Get dashboard overview", 
               description = "Retrieve comprehensive dashboard data with all key metrics")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardOverview() {
        
        log.debug("Fetching dashboard overview");
        DashboardResponse dashboard = dashboardService.getDashboardOverview();
        
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * Get recently auto-closed alerts for transparency
     * GET /api/v1/dashboard/auto-closed
     * 
     * @param hours Number of hours to look back (default: 24)
     * @return List of recently auto-closed alerts
     */
    @GetMapping("/auto-closed")
    @Operation(summary = "Get recently auto-closed alerts", 
               description = "Retrieve alerts that were automatically closed in the specified time period")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getRecentlyAutoClosedAlerts(
            @RequestParam(defaultValue = "24") int hours) {
        
        log.debug("Fetching auto-closed alerts from last {} hours", hours);
        List<AlertResponse> alerts = dashboardService.getRecentlyAutoClosedAlerts(hours);
        
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    /**
     * Get alert trends over time
     * GET /api/v1/dashboard/trends
     * 
     * @param days Number of days to analyze (default: 7)
     * @return Trend data showing alert evolution
     */
    @GetMapping("/trends")
    @Operation(summary = "Get alert trends", 
               description = "Retrieve trend data showing how alerts have evolved over time")
    public ResponseEntity<ApiResponse<List<TrendDataResponse>>> getAlertTrends(
            @RequestParam(defaultValue = "7") int days) {
        
        log.debug("Fetching alert trends for last {} days", days);
        List<TrendDataResponse> trends = dashboardService.getTrendData(days);
        
        return ResponseEntity.ok(ApiResponse.success(trends));
    }

    /**
     * Get alert drill-down details
     * GET /api/v1/dashboard/alert/{alertId}/drilldown
     * 
     * @param alertId Alert ID
     * @return Detailed alert information with history
     */
    @GetMapping("/alert/{alertId}/drilldown")
    @Operation(summary = "Get alert drill-down", 
               description = "Retrieve detailed alert information including complete history")
    public ResponseEntity<ApiResponse<AlertResponse>> getAlertDrillDown(
            @PathVariable String alertId) {
        
        log.debug("Fetching drill-down for alert: {}", alertId);
        AlertResponse alert = dashboardService.getAlertDrillDown(alertId);
        
        return ResponseEntity.ok(ApiResponse.success(alert));
    }

    /**
     * Get alert statistics summary
     * GET /api/v1/dashboard/statistics
     * 
     * @return Statistics summary
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get alert statistics", 
               description = "Retrieve comprehensive alert statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        
        log.debug("Fetching alert statistics");
        Map<String, Object> stats = dashboardService.getAlertStatistics();
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

