package com.movesync.alert.domain.model;

import com.movesync.alert.domain.enums.AlertSeverity;
import com.movesync.alert.domain.enums.AlertStatus;
import com.movesync.alert.domain.enums.AlertType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Core Alert entity representing a single alert in the system
 * Follows the unified format: {alertId, sourceType, severity, timestamp, status, metadata}
 */
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alert_status", columnList = "status"),
    @Index(name = "idx_alert_type", columnList = "alertType"),
    @Index(name = "idx_alert_driver", columnList = "driverId"),
    @Index(name = "idx_alert_timestamp", columnList = "timestamp"),
    @Index(name = "idx_alert_severity", columnList = "severity")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {
    
    @Id
    @Column(name = "alert_id", updatable = false, nullable = false)
    private String alertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status;

    // Entity identifiers
    @Column(name = "driver_id", length = 100)
    private String driverId;

    @Column(name = "vehicle_id", length = 100)
    private String vehicleId;

    @Column(name = "route_id", length = 100)
    private String routeId;

    // Metadata stored as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    // Escalation tracking
    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "escalation_reason", length = 500)
    private String escalationReason;

    // Closure tracking
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closure_reason", length = 500)
    private String closureReason;

    @Column(name = "closed_by", length = 100)
    private String closedBy; // User ID or "SYSTEM" for auto-close

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (alertId == null) {
            alertId = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (status == null) {
            status = AlertStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Business logic: Escalate the alert
     * State transition: OPEN → ESCALATED
     * 
     * @param newSeverity New severity level (typically CRITICAL or WARNING)
     * @param reason Reason for escalation
     * @throws IllegalStateException if alert is already closed
     */
    public void escalate(AlertSeverity newSeverity, String reason) {
        if (this.status.isClosed()) {
            throw new IllegalStateException(
                String.format("Cannot escalate a closed alert. Current status: %s", this.status)
            );
        }
        // Valid transitions: OPEN → ESCALATED
        if (this.status != AlertStatus.OPEN && this.status != AlertStatus.ESCALATED) {
            throw new IllegalStateException(
                String.format("Invalid state for escalation. Current status: %s. Expected: OPEN or ESCALATED", this.status)
            );
        }
        this.status = AlertStatus.ESCALATED;
        this.severity = newSeverity;
        this.escalatedAt = LocalDateTime.now();
        this.escalationReason = reason;
    }

    /**
     * Business logic: Auto-close the alert
     * State transition: OPEN/ESCALATED → AUTO_CLOSED
     * 
     * @param reason Reason for auto-closure
     */
    public void autoClose(String reason) {
        if (this.status.isClosed()) {
            return; // Already closed, idempotent
        }
        // Valid transitions: OPEN → AUTO_CLOSED or ESCALATED → AUTO_CLOSED
        if (this.status != AlertStatus.OPEN && this.status != AlertStatus.ESCALATED) {
            throw new IllegalStateException(
                String.format("Invalid state for auto-close. Current status: %s. Expected: OPEN or ESCALATED", this.status)
            );
        }
        this.status = AlertStatus.AUTO_CLOSED;
        this.closedAt = LocalDateTime.now();
        this.closureReason = reason;
        this.closedBy = "SYSTEM";
    }

    /**
     * Business logic: Manually resolve the alert
     * State transition: OPEN/ESCALATED → RESOLVED
     * 
     * @param userId User ID performing the resolution
     * @param reason Reason for resolution
     */
    public void resolve(String userId, String reason) {
        if (this.status.isClosed()) {
            return; // Already closed, idempotent
        }
        // Valid transitions: OPEN → RESOLVED or ESCALATED → RESOLVED
        if (this.status != AlertStatus.OPEN && this.status != AlertStatus.ESCALATED) {
            throw new IllegalStateException(
                String.format("Invalid state for resolution. Current status: %s. Expected: OPEN or ESCALATED", this.status)
            );
        }
        this.status = AlertStatus.RESOLVED;
        this.closedAt = LocalDateTime.now();
        this.closureReason = reason;
        this.closedBy = userId;
    }

    /**
     * Check if alert has expired based on time window
     */
    public boolean isExpired(int windowMinutes) {
        LocalDateTime expiryTime = timestamp.plusMinutes(windowMinutes);
        return LocalDateTime.now().isAfter(expiryTime);
    }

    /**
     * Add metadata entry
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
}

