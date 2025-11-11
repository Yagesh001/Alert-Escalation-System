package com.movesync.alert.domain.enums;

/**
 * Types of alerts from different source modules
 */
public enum AlertType {
    // Safety Module
    OVERSPEEDING("Safety", "Vehicle exceeded speed limit"),
    HARSH_BRAKING("Safety", "Harsh braking detected"),
    HARSH_ACCELERATION("Safety", "Harsh acceleration detected"),
    ROUTE_DEVIATION("Safety", "Vehicle deviated from route"),
    
    // Compliance Module
    COMPLIANCE_DOCUMENT_EXPIRY("Compliance", "Document expiring or expired"),
    COMPLIANCE_LICENSE_INVALID("Compliance", "Invalid or expired license"),
    COMPLIANCE_INSURANCE_EXPIRY("Compliance", "Insurance expiring"),
    
    // Feedback Module
    FEEDBACK_NEGATIVE("Feedback", "Negative feedback received"),
    FEEDBACK_COMPLAINT("Feedback", "Complaint filed"),
    
    // Maintenance Module
    MAINTENANCE_OVERDUE("Maintenance", "Maintenance overdue"),
    FUEL_THEFT("Maintenance", "Potential fuel theft detected");

    private final String sourceModule;
    private final String description;

    AlertType(String sourceModule, String description) {
        this.sourceModule = sourceModule;
        this.description = description;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    public String getDescription() {
        return description;
    }
}

