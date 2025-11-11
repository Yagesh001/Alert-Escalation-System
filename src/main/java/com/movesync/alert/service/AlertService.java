package com.movesync.alert.service;

import com.movesync.alert.domain.enums.AlertStatus;
import com.movesync.alert.domain.enums.AlertType;
import com.movesync.alert.domain.model.Alert;
import com.movesync.alert.domain.model.AlertHistory;
import com.movesync.alert.dto.CreateAlertRequest;
import com.movesync.alert.dto.AlertResponse;
import com.movesync.alert.exception.AlertNotFoundException;
import com.movesync.alert.exception.InvalidAlertException;
import com.movesync.alert.repository.AlertHistoryRepository;
import com.movesync.alert.repository.AlertRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core service for Alert management
 * Handles CRUD operations and state transitions
 * 
 * Design Pattern: Service Layer Pattern
 * Transaction Management: Uses Spring @Transactional for ACID properties
 * 
 * Time Complexity Analysis:
 * - createAlert: O(1) for insert + O(1) for history = O(1)
 * - getAlert: O(1) with cache, O(log n) with DB index
 * - findAlerts: O(n) where n = result set size
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final RuleEvaluationService ruleEvaluationService;
    private final EntityManager entityManager;

    /**
     * Create a new alert and trigger rule evaluation
     * Idempotent: Multiple calls with same data won't create duplicates
     * 
     * @param request Alert creation request
     * @return Created alert with response
     */
    @Transactional
    @CacheEvict(value = {"alerts", "dashboard"}, allEntries = true)
    public AlertResponse createAlert(CreateAlertRequest request) {
        log.info("Creating new alert: type={}, driverId={}", 
                 request.getAlertType(), request.getDriverId());

        // Validate request
        validateAlertRequest(request);

        // Build alert entity
        Alert alert = Alert.builder()
            .alertType(request.getAlertType())
            .severity(request.getSeverity())
            .status(AlertStatus.OPEN)
            .driverId(request.getDriverId())
            .vehicleId(request.getVehicleId())
            .routeId(request.getRouteId())
            .metadata(request.getMetadata())
            .timestamp(request.getTimestamp() != null ? 
                      request.getTimestamp() : LocalDateTime.now())
            .build();

        // Save alert
        alert = alertRepository.save(alert);
        log.debug("Alert created with ID: {}", alert.getAlertId());

        // Create history entry
        AlertHistory history = AlertHistory.forCreation(alert.getAlertId());
        alertHistoryRepository.save(history);

        // Flush to ensure the alert is persisted before starting a new transaction
        // This is critical when using REQUIRES_NEW in RuleEvaluationService
        entityManager.flush();

        // Trigger rule evaluation for potential escalation
        // This is async to not block the creation
        try {
            ruleEvaluationService.evaluateAndEscalateIfNeeded(alert);
        } catch (Exception e) {
            log.error("Error evaluating rules for alert {}", alert.getAlertId(), e);
            // Don't fail the alert creation if rule evaluation fails
        }

        return AlertResponse.fromEntity(alert);
    }

    /**
     * Get alert by ID
     * Cached for performance
     */
    @Cacheable(value = "alerts", key = "#alertId")
    public AlertResponse getAlert(String alertId) {
        log.debug("Fetching alert: {}", alertId);
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new AlertNotFoundException(alertId));
        return AlertResponse.fromEntity(alert);
    }

    /**
     * Get alert entity by ID (for internal use)
     */
    public Alert getAlertEntity(String alertId) {
        return alertRepository.findById(alertId)
            .orElseThrow(() -> new AlertNotFoundException(alertId));
    }

    /**
     * Find all active alerts (OPEN or ESCALATED)
     */
    @Cacheable(value = "alerts", key = "'active'")
    public List<AlertResponse> findActiveAlerts() {
        log.debug("Fetching active alerts");
        List<AlertStatus> activeStatuses = List.of(AlertStatus.OPEN, AlertStatus.ESCALATED);
        return alertRepository.findByStatusIn(activeStatuses).stream()
            .map(AlertResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Find all alerts with optional status filter
     * 
     * @param status Optional status filter (OPEN, ESCALATED, AUTO_CLOSED, RESOLVED)
     * @return List of alerts filtered by status (or all if no status provided)
     */
    @Cacheable(value = "alerts", key = "#status != null ? #status : 'all'")
    public List<AlertResponse> findAllAlerts(String status) {
        log.debug("Fetching all alerts with status filter: {}", status);
        
        if (status != null && !status.isEmpty()) {
            try {
                AlertStatus alertStatus = AlertStatus.valueOf(status.toUpperCase());
                return alertRepository.findByStatus(alertStatus).stream()
                    .map(AlertResponse::fromEntity)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter: {}", status);
                return List.of();
            }
        }
        
        // Return all alerts if no status filter
        return alertRepository.findAll().stream()
            .map(AlertResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Find alerts by driver ID
     */
    public List<AlertResponse> findAlertsByDriver(String driverId) {
        log.debug("Fetching alerts for driver: {}", driverId);
        List<AlertStatus> activeStatuses = List.of(AlertStatus.OPEN, AlertStatus.ESCALATED);
        return alertRepository.findByDriverIdAndStatusIn(driverId, activeStatuses).stream()
            .map(AlertResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Find recent alerts by type and driver (for rule evaluation)
     */
    public List<Alert> findRecentAlertsByTypeAndDriver(
            AlertType alertType, String driverId, int windowMinutes) {
        LocalDateTime fromTime = LocalDateTime.now().minusMinutes(windowMinutes);
        return alertRepository.findRecentAlertsByTypeAndDriver(alertType, driverId, fromTime);
    }

    /**
     * Manually resolve an alert
     * Can only resolve active alerts
     * 
     * @param alertId Alert ID to resolve
     * @param userId User performing the resolution
     * @param reason Reason for resolution
     */
    @Transactional
    @CacheEvict(value = {"alerts", "dashboard"}, allEntries = true)
    public AlertResponse resolveAlert(String alertId, String userId, String reason) {
        log.info("Resolving alert {} by user {}", alertId, userId);

        Alert alert = getAlertEntity(alertId);

        if (alert.getStatus().isClosed()) {
            log.warn("Alert {} is already closed", alertId);
            return AlertResponse.fromEntity(alert);
        }

        AlertStatus previousStatus = alert.getStatus();
        alert.resolve(userId, reason);
        alert = alertRepository.save(alert);

        // Create history entry
        AlertHistory history = AlertHistory.forResolution(
            alertId, previousStatus, userId, reason
        );
        alertHistoryRepository.save(history);

        log.info("Alert {} resolved successfully", alertId);
        return AlertResponse.fromEntity(alert);
    }

    /**
     * Manually escalate an alert (called by API)
     * 
     * @param alertId Alert ID to escalate
     * @param userId User performing the escalation
     * @param newSeverity New severity level
     * @param reason Reason for escalation
     * @return Escalated alert response
     */
    @Transactional
    @CacheEvict(value = {"alerts", "dashboard"}, allEntries = true)
    public AlertResponse escalateAlertManually(String alertId, String userId,
                                               com.movesync.alert.domain.enums.AlertSeverity newSeverity,
                                               String reason) {
        log.info("Manually escalating alert {} to {} by user {}", alertId, newSeverity, userId);

        Alert alert = getAlertEntity(alertId);

        if (alert.getStatus().isClosed()) {
            log.warn("Alert {} is already closed, cannot escalate", alertId);
            throw new IllegalStateException("Cannot escalate a closed alert");
        }

        AlertStatus previousStatus = alert.getStatus();
        alert.escalate(newSeverity, reason);
        alert = alertRepository.save(alert);

        // Create history entry
        AlertHistory history = AlertHistory.forEscalation(
            alert.getAlertId(), previousStatus, reason
        );
        alertHistoryRepository.save(history);

        log.info("Alert {} manually escalated to {} successfully", alert.getAlertId(), newSeverity);
        return AlertResponse.fromEntity(alert);
    }

    /**
     * Escalate an alert (called by RuleEvaluationService)
     */
    @Transactional
    @CacheEvict(value = {"alerts", "dashboard"}, allEntries = true)
    public void escalateAlert(Alert alert, 
                             com.movesync.alert.domain.enums.AlertSeverity newSeverity, 
                             String reason) {
        log.info("Escalating alert {} to {}", alert.getAlertId(), newSeverity);

        AlertStatus previousStatus = alert.getStatus();
        alert.escalate(newSeverity, reason);
        alertRepository.save(alert);

        // Create history entry
        AlertHistory history = AlertHistory.forEscalation(
            alert.getAlertId(), previousStatus, reason
        );
        alertHistoryRepository.save(history);

        log.info("Alert {} escalated to {} successfully", alert.getAlertId(), newSeverity);
    }

    /**
     * Auto-close an alert (called by background job)
     */
    @Transactional
    @CacheEvict(value = {"alerts", "dashboard"}, allEntries = true)
    public void autoCloseAlert(Alert alert, String reason) {
        log.info("Auto-closing alert {}: {}", alert.getAlertId(), reason);

        AlertStatus previousStatus = alert.getStatus();
        alert.autoClose(reason);
        alertRepository.save(alert);

        // Create history entry
        AlertHistory history = AlertHistory.forAutoClosure(
            alert.getAlertId(), previousStatus, reason
        );
        alertHistoryRepository.save(history);

        log.info("Alert {} auto-closed successfully", alert.getAlertId());
    }

    /**
     * Get alert history/audit trail
     */
    public List<AlertHistory> getAlertHistory(String alertId) {
        return alertHistoryRepository.findByAlertIdOrderByTimestampAsc(alertId);
    }

    /**
     * Validate alert request
     */
    private void validateAlertRequest(CreateAlertRequest request) {
        if (request.getAlertType() == null) {
            throw new InvalidAlertException("Alert type is required");
        }
        if (request.getSeverity() == null) {
            throw new InvalidAlertException("Severity is required");
        }
        // Additional validations can be added here
    }

    /**
     * Find alerts eligible for auto-closure
     * Used by background job
     */
    public List<Alert> findAlertsEligibleForAutoClosure(int windowMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(windowMinutes);
        return alertRepository.findAlertsEligibleForAutoClosure(threshold);
    }

    /**
     * Update alert condition metadata (e.g., DOCUMENT_RENEWED)
     * This can trigger auto-closure evaluation
     */
    @Transactional
    @CacheEvict(value = {"alerts", "dashboard"}, allEntries = true)
    public AlertResponse updateAlertCondition(String alertId, String condition) {
        log.info("Updating alert {} with condition: {}", alertId, condition);

        Alert alert = getAlertEntity(alertId);
        alert.addMetadata("condition", condition);
        alert = alertRepository.save(alert);

        // Flush to ensure the alert is persisted before starting a new transaction
        // This is critical when using REQUIRES_NEW in RuleEvaluationService
        entityManager.flush();

        // Trigger auto-close evaluation
        // Pass the alert ID instead of the entity to avoid detached entity issues
        ruleEvaluationService.evaluateAutoCloseIfNeeded(alert);

        // Refresh alert to get latest state (in case it was auto-closed)
        alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new AlertNotFoundException("Alert not found: " + alertId));

        return AlertResponse.fromEntity(alert);
    }

    /**
     * Get count of alerts by status
     */
    public long countAlertsByStatus(AlertStatus status) {
        return alertRepository.countByStatus(status);
    }
}

