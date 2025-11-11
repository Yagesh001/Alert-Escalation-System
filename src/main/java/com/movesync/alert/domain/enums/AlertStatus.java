package com.movesync.alert.domain.enums;

/**
 * Enum representing the lifecycle states of an Alert
 * State Transitions: OPEN → ESCALATED → AUTO_CLOSED/RESOLVED
 */
public enum AlertStatus {
    OPEN("Open - newly created alert"),
    ESCALATED("Escalated - met escalation criteria"),
    AUTO_CLOSED("Auto-closed by system based on rules"),
    RESOLVED("Manually resolved by operator");

    private final String description;

    AlertStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == OPEN || this == ESCALATED;
    }

    public boolean isClosed() {
        return this == AUTO_CLOSED || this == RESOLVED;
    }
}

