package com.movesync.alert.controller;

import com.movesync.alert.domain.model.AlertHistory;
import com.movesync.alert.dto.AlertResponse;
import com.movesync.alert.dto.ApiResponse;
import com.movesync.alert.dto.CreateAlertRequest;
import com.movesync.alert.dto.ResolveAlertRequest;
import com.movesync.alert.dto.EscalateAlertRequest;
import com.movesync.alert.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Alert operations
 * Handles alert creation, retrieval, and resolution
 * 
 * API Design:
 * - RESTful endpoints
 * - Standard HTTP status codes
 * - JWT authentication required
 * 
 * Rate Limiting: Can be added via Spring Cloud Gateway or custom interceptor
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Management", description = "APIs for managing alerts")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    /**
     * Create a new alert
     * POST /api/v1/alerts
     * 
     * @param request Alert creation request
     * @return Created alert response
     */
    @PostMapping
    @Operation(summary = "Create new alert", 
               description = "Ingest a new alert from source modules (Safety, Compliance, Feedback)")
    public ResponseEntity<ApiResponse<AlertResponse>> createAlert(
            @Valid @RequestBody CreateAlertRequest request) {
        
        log.info("Received request to create alert: type={}, driverId={}", 
                 request.getAlertType(), request.getDriverId());

        AlertResponse response = alertService.createAlert(request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Alert created successfully", response));
    }

    /**
     * Get alert by ID
     * GET /api/v1/alerts/{alertId}
     * 
     * @param alertId Alert ID
     * @return Alert details
     */
    @GetMapping("/{alertId}")
    @Operation(summary = "Get alert by ID", 
               description = "Retrieve detailed information about a specific alert")
    public ResponseEntity<ApiResponse<AlertResponse>> getAlert(
            @PathVariable String alertId) {
        
        log.debug("Fetching alert: {}", alertId);
        AlertResponse response = alertService.getAlert(alertId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all active alerts
     * GET /api/v1/alerts/active
     * 
     * @return List of active alerts
     */
    @GetMapping("/active")
    @Operation(summary = "Get all active alerts", 
               description = "Retrieve all OPEN and ESCALATED alerts")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getActiveAlerts() {
        
        log.debug("Fetching active alerts");
        List<AlertResponse> alerts = alertService.findActiveAlerts();
        
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    /**
     * Get all alerts with optional status filter
     * GET /api/v1/alerts/all?status=OPEN
     * 
     * @param status Optional status filter (OPEN, ESCALATED, AUTO_CLOSED, RESOLVED)
     * @return List of alerts filtered by status (or all if no status provided)
     */
    @GetMapping("/all")
    @Operation(summary = "Get all alerts with optional status filter", 
               description = "Retrieve all alerts, optionally filtered by status")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAllAlerts(
            @RequestParam(required = false) String status) {
        
        log.debug("Fetching all alerts with status filter: {}", status);
        List<AlertResponse> alerts = alertService.findAllAlerts(status);
        
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    /**
     * Get alerts by driver
     * GET /api/v1/alerts/driver/{driverId}
     * 
     * @param driverId Driver ID
     * @return List of alerts for the driver
     */
    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get alerts by driver", 
               description = "Retrieve all alerts for a specific driver")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlertsByDriver(
            @PathVariable String driverId) {
        
        log.debug("Fetching alerts for driver: {}", driverId);
        List<AlertResponse> alerts = alertService.findAlertsByDriver(driverId);
        
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    /**
     * Manually resolve an alert
     * PUT /api/v1/alerts/{alertId}/resolve
     * 
     * @param alertId Alert ID to resolve
     * @param request Resolution request with reason
     * @param userDetails Authenticated user
     * @return Resolved alert
     */
    @PutMapping("/{alertId}/resolve")
    @Operation(summary = "Resolve alert", 
               description = "Manually resolve an active alert")
    public ResponseEntity<ApiResponse<AlertResponse>> resolveAlert(
            @PathVariable String alertId,
            @Valid @RequestBody ResolveAlertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userId = userDetails.getUsername();
        log.info("Resolving alert {} by user {}", alertId, userId);

        AlertResponse response = alertService.resolveAlert(alertId, userId, request.getReason());
        
        return ResponseEntity.ok(ApiResponse.success("Alert resolved successfully", response));
    }

    /**
     * Get alert history/audit trail
     * GET /api/v1/alerts/{alertId}/history
     * 
     * @param alertId Alert ID
     * @return Alert history
     */
    @GetMapping("/{alertId}/history")
    @Operation(summary = "Get alert history", 
               description = "Retrieve the complete audit trail of alert state transitions")
    public ResponseEntity<ApiResponse<List<AlertHistory>>> getAlertHistory(
            @PathVariable String alertId) {
        
        log.debug("Fetching history for alert: {}", alertId);
        List<AlertHistory> history = alertService.getAlertHistory(alertId);
        
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * Manually escalate an alert
     * PUT /api/v1/alerts/{alertId}/escalate
     * 
     * @param alertId Alert ID to escalate
     * @param request Escalation request with severity and reason
     * @return Escalated alert
     */
    @PutMapping("/{alertId}/escalate")
    @Operation(summary = "Manually escalate alert", 
               description = "Manually escalate an active alert to a specific severity level")
    public ResponseEntity<ApiResponse<AlertResponse>> escalateAlert(
            @PathVariable String alertId,
            @Valid @RequestBody EscalateAlertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userId = userDetails.getUsername();
        log.info("Manually escalating alert {} to {} by user {}", alertId, request.getSeverity(), userId);

        AlertResponse response = alertService.escalateAlertManually(alertId, userId, request.getSeverity(), request.getReason());
        
        return ResponseEntity.ok(ApiResponse.success("Alert escalated successfully", response));
    }

    /**
     * Update alert condition (e.g., DOCUMENT_RENEWED)
     * PATCH /api/v1/alerts/{alertId}/condition
     * 
     * @param alertId Alert ID
     * @param condition Condition to set
     * @return Updated alert
     */
    @PatchMapping("/{alertId}/condition")
    @Operation(summary = "Update alert condition", 
               description = "Update alert metadata with condition (can trigger auto-closure)")
    public ResponseEntity<ApiResponse<AlertResponse>> updateAlertCondition(
            @PathVariable String alertId,
            @RequestParam String condition) {
        
        log.info("Updating alert {} with condition: {}", alertId, condition);
        AlertResponse response = alertService.updateAlertCondition(alertId, condition);
        
        return ResponseEntity.ok(ApiResponse.success("Alert condition updated", response));
    }
}

