package com.movesync.alert.engine;

import com.movesync.alert.domain.enums.AlertType;
import com.movesync.alert.domain.model.Alert;
import com.movesync.alert.domain.model.EscalationRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Core Rule Engine that evaluates escalation and auto-closure conditions
 * Implements the DSL-based rule system with dynamic evaluation
 * 
 * Design Principles:
 * - Rules are decoupled from alert logic
 * - Evaluation is stateless and idempotent
 * - Supports dynamic rule updates without code changes
 * 
 * Time Complexity: O(1) for rule lookup (cached), O(n) for alert evaluation where n = number of matching alerts
 * Space Complexity: O(r) where r = number of rules (cached in memory)
 */
@Slf4j
@Component
public class RuleEngine {

    private final RuleLoader ruleLoader;

    public RuleEngine(RuleLoader ruleLoader) {
        this.ruleLoader = ruleLoader;
    }

    /**
     * Evaluate if alerts should be escalated based on rules
     * 
     * @param alertType The type of alert to evaluate
     * @param recentAlerts List of recent alerts of same type for same entity
     * @return EscalationDecision containing whether to escalate and details
     */
    public EscalationDecision evaluateEscalation(AlertType alertType, List<Alert> recentAlerts) {
        log.debug("Evaluating escalation for alertType: {} with {} recent alerts", 
                  alertType, recentAlerts.size());

        Optional<EscalationRule> ruleOpt = getRuleForAlertType(alertType);
        
        if (ruleOpt.isEmpty()) {
            log.warn("No rule found for alert type: {}", alertType);
            return EscalationDecision.noEscalation("No rule configured");
        }

        EscalationRule rule = ruleOpt.get();
        
        if (!rule.getEnabled()) {
            return EscalationDecision.noEscalation("Rule is disabled");
        }

        // Filter alerts within the time window
        // Use isAfter with a small buffer to ensure newly created alerts are included
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(rule.getWindowMinutes());
        List<Alert> alertsInWindow = recentAlerts.stream()
            .filter(a -> !a.getTimestamp().isBefore(windowStart)) // Include alerts at or after windowStart
            .toList();

        int alertCount = alertsInWindow.size();
        
        if (alertCount < rule.getEscalateIfCount()) {
            log.debug("Alert count {} is below threshold {} for {}", 
                      alertCount, rule.getEscalateIfCount(), alertType);
            return EscalationDecision.noEscalation(
                String.format("Count %d below threshold %d", alertCount, rule.getEscalateIfCount())
            );
        }

        // Calculate time difference
        if (alertsInWindow.isEmpty()) {
            return EscalationDecision.noEscalation("No alerts in window");
        }

        LocalDateTime firstAlertTime = alertsInWindow.get(alertsInWindow.size() - 1).getTimestamp();
        LocalDateTime lastAlertTime = alertsInWindow.get(0).getTimestamp();
        long timeDifferenceMinutes = ChronoUnit.MINUTES.between(firstAlertTime, lastAlertTime);

        if (rule.shouldEscalate(alertCount, timeDifferenceMinutes)) {
            String reason = String.format(
                "%d occurrences of %s within %d minutes (threshold: %d in %d minutes)",
                alertCount, alertType, timeDifferenceMinutes, 
                rule.getEscalateIfCount(), rule.getWindowMinutes()
            );
            
            log.info("Escalation triggered: {}", reason);
            return EscalationDecision.escalate(rule.getEscalationSeverity(), reason);
        }

        return EscalationDecision.noEscalation("Conditions not met");
    }

    /**
     * Evaluate if an alert should be auto-closed
     * 
     * @param alert The alert to evaluate
     * @param recentAlerts Recent alerts of same type for same entity
     * @return AutoCloseDecision containing whether to close and reason
     */
    public AutoCloseDecision evaluateAutoClose(Alert alert, List<Alert> recentAlerts) {
        log.debug("Evaluating auto-close for alert: {}", alert.getAlertId());

        Optional<EscalationRule> ruleOpt = getRuleForAlertType(alert.getAlertType());
        
        if (ruleOpt.isEmpty()) {
            return AutoCloseDecision.noClose("No rule configured");
        }

        EscalationRule rule = ruleOpt.get();

        // Check condition-based auto-close (e.g., DOCUMENT_RENEWED)
        if (rule.getAutoCloseIf() != null && !rule.getAutoCloseIf().isEmpty()) {
            // Check if condition is met in metadata
            Object conditionValue = alert.getMetadata().get("condition");
            if (conditionValue != null && rule.shouldAutoCloseByCondition(conditionValue.toString())) {
                String reason = String.format("Condition met: %s", rule.getAutoCloseIf());
                log.info("Auto-close triggered by condition: {}", reason);
                return AutoCloseDecision.close(reason);
            }
        }

        // Check time-based auto-close (no repeat within window)
        if (rule.getAutoCloseIfNoRepeat() != null && rule.getAutoCloseIfNoRepeat()) {
            long minutesSinceAlert = ChronoUnit.MINUTES.between(alert.getTimestamp(), LocalDateTime.now());
            
            if (minutesSinceAlert >= rule.getAutoCloseWindowMinutes()) {
                // Check if there are no new alerts in the auto-close window
                boolean hasRecentAlerts = recentAlerts.stream()
                    .anyMatch(a -> a.getTimestamp().isAfter(alert.getTimestamp()) 
                                && a.getTimestamp().isBefore(LocalDateTime.now()));

                if (!hasRecentAlerts) {
                    String reason = String.format(
                        "No repeat within %d minutes (window expired)",
                        rule.getAutoCloseWindowMinutes()
                    );
                    log.info("Auto-close triggered by time window: {}", reason);
                    return AutoCloseDecision.close(reason);
                }
            }
        }

        return AutoCloseDecision.noClose("Conditions not met");
    }

    /**
     * Get rule for specific alert type (cached for performance)
     */
    @Cacheable(value = "rules", key = "#alertType")
    public Optional<EscalationRule> getRuleForAlertType(AlertType alertType) {
        return ruleLoader.getRules().stream()
            .filter(rule -> rule.getAlertType() == alertType)
            .findFirst();
    }

    /**
     * Reload rules from configuration (cache eviction handled by RuleLoader)
     */
    public void reloadRules() {
        ruleLoader.loadRules();
        log.info("Rules reloaded successfully");
    }

    /**
     * Get all active rules
     */
    public List<EscalationRule> getAllRules() {
        return ruleLoader.getRules();
    }

    /**
     * Inner class representing escalation decision
     */
    public static class EscalationDecision {
        private final boolean shouldEscalate;
        private final com.movesync.alert.domain.enums.AlertSeverity newSeverity;
        private final String reason;

        private EscalationDecision(boolean shouldEscalate, 
                                  com.movesync.alert.domain.enums.AlertSeverity newSeverity, 
                                  String reason) {
            this.shouldEscalate = shouldEscalate;
            this.newSeverity = newSeverity;
            this.reason = reason;
        }

        public static EscalationDecision escalate(com.movesync.alert.domain.enums.AlertSeverity severity, 
                                                  String reason) {
            return new EscalationDecision(true, severity, reason);
        }

        public static EscalationDecision noEscalation(String reason) {
            return new EscalationDecision(false, null, reason);
        }

        public boolean shouldEscalate() {
            return shouldEscalate;
        }

        public com.movesync.alert.domain.enums.AlertSeverity getNewSeverity() {
            return newSeverity;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Inner class representing auto-close decision
     */
    public static class AutoCloseDecision {
        private final boolean shouldClose;
        private final String reason;

        private AutoCloseDecision(boolean shouldClose, String reason) {
            this.shouldClose = shouldClose;
            this.reason = reason;
        }

        public static AutoCloseDecision close(String reason) {
            return new AutoCloseDecision(true, reason);
        }

        public static AutoCloseDecision noClose(String reason) {
            return new AutoCloseDecision(false, reason);
        }

        public boolean shouldClose() {
            return shouldClose;
        }

        public String getReason() {
            return reason;
        }
    }
}

