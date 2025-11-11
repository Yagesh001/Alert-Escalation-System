package com.movesync.alert.domain.model;

import com.movesync.alert.domain.enums.AlertSeverity;
import com.movesync.alert.domain.enums.AlertType;
import lombok.*;

/**
 * POJO representing a rule for alert escalation and auto-closure
 * Loaded from rules.json configuration file
 * Supports DSL-like configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscalationRule {
    
    private AlertType alertType;
    
    // Escalation criteria
    private Integer escalateIfCount;
    private Integer windowMinutes;
    private AlertSeverity escalationSeverity;
    
    // Auto-closure criteria
    private Boolean autoCloseIfNoRepeat;
    private String autoCloseIf; // Condition like "DOCUMENT_RENEWED"
    private Integer autoCloseWindowMinutes;
    
    // Additional configuration
    @Builder.Default
    private Boolean enabled = true;
    @Builder.Default
    private Integer priority = 0;

    /**
     * Check if this rule should trigger escalation
     */
    public boolean shouldEscalate(int alertCount, long timeDifferenceMinutes) {
        if (!enabled || escalateIfCount == null || windowMinutes == null) {
            return false;
        }
        return alertCount >= escalateIfCount && timeDifferenceMinutes <= windowMinutes;
    }

    /**
     * Check if alert should auto-close based on time window
     */
    public boolean shouldAutoCloseByTime(long minutesSinceLastAlert) {
        if (!enabled || autoCloseIfNoRepeat == null || !autoCloseIfNoRepeat) {
            return false;
        }
        return autoCloseWindowMinutes != null && minutesSinceLastAlert >= autoCloseWindowMinutes;
    }

    /**
     * Check if alert should auto-close based on condition
     */
    public boolean shouldAutoCloseByCondition(String condition) {
        if (!enabled || autoCloseIf == null || autoCloseIf.isEmpty()) {
            return false;
        }
        return autoCloseIf.equalsIgnoreCase(condition);
    }

    /**
     * Get escalation window in minutes
     */
    public int getEscalationWindowMinutes() {
        return windowMinutes != null ? windowMinutes : 60; // Default 1 hour
    }

    /**
     * Get auto-close window in minutes
     */
    public int getAutoCloseWindowMinutes() {
        return autoCloseWindowMinutes != null ? autoCloseWindowMinutes : 120; // Default 2 hours
    }
}

