package com.movesync.alert.domain.enums;

/**
 * Alert severity levels
 */
public enum AlertSeverity {
    INFO(1, "Informational"),
    WARNING(2, "Warning - requires attention"),
    CRITICAL(3, "Critical - requires immediate action");

    private final int priority;
    private final String description;

    AlertSeverity(int priority, String description) {
        this.priority = priority;
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }

    public boolean isMoreSevereThan(AlertSeverity other) {
        return this.priority > other.priority;
    }
}

