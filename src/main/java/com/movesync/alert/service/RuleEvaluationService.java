package com.movesync.alert.service;

import com.movesync.alert.domain.model.Alert;
import com.movesync.alert.engine.RuleEngine;
import com.movesync.alert.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for evaluating rules and triggering escalations/auto-closures
 * Decoupled from AlertService to maintain single responsibility
 * 
 * Design Pattern: Strategy Pattern via RuleEngine
 * 
 * Time Complexity: O(n) where n = number of recent alerts for evaluation
 * Space Complexity: O(1) - only stores references
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEvaluationService {

    private final RuleEngine ruleEngine;
    private final AlertRepository alertRepository;
    private final com.movesync.alert.repository.AlertHistoryRepository alertHistoryRepository;

    /**
     * Evaluate and escalate alert if rules are met
     * Called when new alert is created
     * 
     * @param alert The newly created alert
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluateAndEscalateIfNeeded(Alert alert) {
        log.debug("Evaluating escalation rules for alert: {}", alert.getAlertId());

        try {
            // Get recent alerts of same type for same driver
            var ruleOpt = ruleEngine.getRuleForAlertType(alert.getAlertType());
            
            if (ruleOpt.isEmpty()) {
                log.debug("No rule found for alert type: {}", alert.getAlertType());
                return;
            }

            int windowMinutes = ruleOpt.get().getWindowMinutes();
            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(windowMinutes);
            
            List<Alert> recentAlerts = alertRepository.findRecentAlertsByTypeAndDriver(
                alert.getAlertType(),
                alert.getDriverId(),
                windowStart
            );

            log.debug("Found {} recent alerts from query for driver {} and type {}", 
                     recentAlerts.size(), alert.getDriverId(), alert.getAlertType());

            // IMPORTANT: Include the newly created alert in the evaluation
            // Since we're in a new transaction (REQUIRES_NEW), we need to fetch it from DB
            // to ensure it's a managed entity in this transaction context
            Alert newAlertInTransaction = alertRepository.findById(alert.getAlertId()).orElse(null);
            if (newAlertInTransaction != null) {
                // Check if it's already in the list
                boolean containsNewAlert = recentAlerts.stream()
                    .anyMatch(a -> a.getAlertId().equals(alert.getAlertId()));
                if (!containsNewAlert) {
                    recentAlerts.add(newAlertInTransaction);
                    log.info("Added newly created alert {} to recent alerts list. Total alerts for evaluation: {}", 
                            alert.getAlertId(), recentAlerts.size());
                } else {
                    log.debug("Newly created alert {} already in recent alerts list", alert.getAlertId());
                }
            } else {
                log.warn("Newly created alert {} not found in database. This might be due to transaction isolation.", 
                        alert.getAlertId());
                // Add the passed alert for evaluation, but we'll fetch it fresh when escalating
                boolean containsNewAlert = recentAlerts.stream()
                    .anyMatch(a -> a.getAlertId().equals(alert.getAlertId()));
                if (!containsNewAlert) {
                    recentAlerts.add(alert);
                    log.info("Added passed alert {} to recent alerts list (not yet in DB). Total alerts for evaluation: {}", 
                            alert.getAlertId(), recentAlerts.size());
                }
            }

            // Evaluate escalation with the complete list (including new alert)
            log.debug("Evaluating escalation with {} total alerts (including newly created)", recentAlerts.size());
            RuleEngine.EscalationDecision decision = 
                ruleEngine.evaluateEscalation(alert.getAlertType(), recentAlerts);

            if (decision.shouldEscalate()) {
                log.info("Escalation triggered! Alert {}: {}", 
                        alert.getAlertId(), decision.getReason());
                log.info("Escalating {} alerts to severity: {}", 
                        recentAlerts.size(), decision.getNewSeverity());
                
                // Escalate all alerts in the window that are still OPEN (including the new one)
                for (Alert recentAlert : recentAlerts) {
                    // Always fetch fresh from DB to ensure we have a managed entity
                    // This prevents primary key violations when saving
                    Alert alertToEscalate = alertRepository.findById(recentAlert.getAlertId())
                        .orElse(null);
                    
                    if (alertToEscalate == null) {
                        log.warn("Alert {} not found in database, skipping escalation", recentAlert.getAlertId());
                        continue;
                    }
                    
                    if (alertToEscalate.getStatus().isActive() && 
                        !alertToEscalate.getStatus().equals(
                            com.movesync.alert.domain.enums.AlertStatus.ESCALATED)) {
                        
                        // Capture status before escalation for history
                        com.movesync.alert.domain.enums.AlertStatus previousStatus = 
                            alertToEscalate.getStatus();
                        
                        // Escalate the alert (this modifies the managed entity)
                        alertToEscalate.escalate(decision.getNewSeverity(), decision.getReason());
                        
                        // Save the managed entity (this will UPDATE, not INSERT)
                        alertRepository.save(alertToEscalate);
                        
                        // Create history entry for audit trail
                        com.movesync.alert.domain.model.AlertHistory history = 
                            com.movesync.alert.domain.model.AlertHistory.forEscalation(
                                alertToEscalate.getAlertId(), 
                                previousStatus,
                                decision.getReason()
                            );
                        alertHistoryRepository.save(history);
                        
                        log.info("Escalated alert {} to {} - Status: {}, Severity: {}", 
                                alertToEscalate.getAlertId(), 
                                decision.getNewSeverity(),
                                alertToEscalate.getStatus(),
                                alertToEscalate.getSeverity());
                    }
                }
            } else {
                log.debug("No escalation needed: {}", decision.getReason());
            }

        } catch (Exception e) {
            log.error("Error evaluating escalation for alert {}", alert.getAlertId(), e);
            // Don't propagate exception - escalation failure shouldn't fail alert creation
        }
    }

    /**
     * Evaluate and auto-close alert if conditions are met
     * Called by background job or when condition changes
     * 
     * @param alert The alert to evaluate (may be detached, will be fetched fresh)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluateAutoCloseIfNeeded(Alert alert) {
        log.debug("Evaluating auto-close rules for alert: {}", alert.getAlertId());

        try {
            // Always fetch fresh from DB to ensure we have a managed entity
            // This prevents issues when called from a different transaction
            Alert alertToEvaluate = alertRepository.findById(alert.getAlertId())
                .orElse(null);
            
            if (alertToEvaluate == null) {
                log.warn("Alert {} not found in database, skipping auto-close evaluation", alert.getAlertId());
                return;
            }

            if (alertToEvaluate.getStatus().isClosed()) {
                log.debug("Alert {} is already closed", alertToEvaluate.getAlertId());
                return;
            }

            // Get recent alerts of same type for same driver
            var ruleOpt = ruleEngine.getRuleForAlertType(alertToEvaluate.getAlertType());
            
            if (ruleOpt.isEmpty()) {
                log.debug("No rule found for alert type: {}", alertToEvaluate.getAlertType());
                return;
            }

            LocalDateTime windowStart = alertToEvaluate.getTimestamp();
            
            List<Alert> recentAlerts = alertRepository.findRecentAlertsByTypeAndDriver(
                alertToEvaluate.getAlertType(),
                alertToEvaluate.getDriverId(),
                windowStart
            );

            // Evaluate auto-close
            RuleEngine.AutoCloseDecision decision = 
                ruleEngine.evaluateAutoClose(alertToEvaluate, recentAlerts);

            if (decision.shouldClose()) {
                log.info("Auto-close needed for alert {}: {}", 
                        alertToEvaluate.getAlertId(), decision.getReason());
                
                // Capture previous status BEFORE auto-closing
                com.movesync.alert.domain.enums.AlertStatus previousStatus = alertToEvaluate.getStatus();
                
                // Auto-close the managed entity
                alertToEvaluate.autoClose(decision.getReason());
                alertRepository.save(alertToEvaluate);

                // Create history entry for audit trail
                com.movesync.alert.domain.model.AlertHistory history = 
                    com.movesync.alert.domain.model.AlertHistory.forAutoClosure(
                        alertToEvaluate.getAlertId(), 
                        previousStatus,
                        decision.getReason()
                    );
                alertHistoryRepository.save(history);

                log.info("Auto-closed alert: {} - Reason: {}", alertToEvaluate.getAlertId(), decision.getReason());
            } else {
                log.debug("No auto-close needed: {}", decision.getReason());
            }

        } catch (Exception e) {
            log.error("Error evaluating auto-close for alert {}", alert.getAlertId(), e);
            // Don't propagate exception - auto-close failure shouldn't fail the condition update
        }
    }

    /**
     * Batch evaluate auto-close for multiple alerts
     * Used by background job
     * Idempotent: Safe to run multiple times
     * 
     * @param alerts List of alerts to evaluate
     * @return Number of alerts auto-closed
     */
    public int batchEvaluateAutoClose(List<Alert> alerts) {
        log.info("Batch evaluating auto-close for {} alerts", alerts.size());
        
        int closedCount = 0;
        for (Alert alert : alerts) {
            try {
                com.movesync.alert.domain.enums.AlertStatus statusBefore = alert.getStatus();
                evaluateAutoCloseIfNeeded(alert);
                
                // Refresh alert to check if status changed
                Alert refreshed = alertRepository.findById(alert.getAlertId()).orElse(null);
                if (refreshed != null && 
                    statusBefore.isActive() && 
                    refreshed.getStatus().isClosed()) {
                    closedCount++;
                }
            } catch (Exception e) {
                log.error("Error in batch auto-close for alert {}", alert.getAlertId(), e);
                // Continue with next alert
            }
        }
        
        log.info("Batch auto-close completed: {} alerts closed out of {} evaluated", 
                closedCount, alerts.size());
        return closedCount;
    }
}

