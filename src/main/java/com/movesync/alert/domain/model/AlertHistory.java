package com.movesync.alert.domain.model;

import com.movesync.alert.domain.enums.AlertStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity to track alert state transitions and lifecycle events
 * Provides audit trail for alert changes
 */
@Entity
@Table(name = "alert_history", indexes = {
    @Index(name = "idx_history_alert", columnList = "alertId"),
    @Index(name = "idx_history_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertHistory {
    
    @Id
    @Column(name = "history_id", updatable = false, nullable = false)
    private String historyId;

    @Column(name = "alert_id", nullable = false)
    private String alertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private AlertStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private AlertStatus toStatus;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "changed_by", length = 100)
    private String changedBy; // User ID or "SYSTEM"

    @Column(name = "event_type", length = 50)
    private String eventType; // CREATED, ESCALATED, AUTO_CLOSED, RESOLVED

    @PrePersist
    protected void onCreate() {
        if (historyId == null) {
            historyId = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    /**
     * Factory method to create history entry for alert creation
     */
    public static AlertHistory forCreation(String alertId) {
        return AlertHistory.builder()
                .alertId(alertId)
                .fromStatus(null)
                .toStatus(AlertStatus.OPEN)
                .timestamp(LocalDateTime.now())
                .eventType("CREATED")
                .changedBy("SYSTEM")
                .reason("Alert created")
                .build();
    }

    /**
     * Factory method to create history entry for escalation
     */
    public static AlertHistory forEscalation(String alertId, AlertStatus fromStatus, String reason) {
        return AlertHistory.builder()
                .alertId(alertId)
                .fromStatus(fromStatus)
                .toStatus(AlertStatus.ESCALATED)
                .timestamp(LocalDateTime.now())
                .eventType("ESCALATED")
                .changedBy("SYSTEM")
                .reason(reason)
                .build();
    }

    /**
     * Factory method to create history entry for auto-closure
     */
    public static AlertHistory forAutoClosure(String alertId, AlertStatus fromStatus, String reason) {
        return AlertHistory.builder()
                .alertId(alertId)
                .fromStatus(fromStatus)
                .toStatus(AlertStatus.AUTO_CLOSED)
                .timestamp(LocalDateTime.now())
                .eventType("AUTO_CLOSED")
                .changedBy("SYSTEM")
                .reason(reason)
                .build();
    }

    /**
     * Factory method to create history entry for manual resolution
     */
    public static AlertHistory forResolution(String alertId, AlertStatus fromStatus, String userId, String reason) {
        return AlertHistory.builder()
                .alertId(alertId)
                .fromStatus(fromStatus)
                .toStatus(AlertStatus.RESOLVED)
                .timestamp(LocalDateTime.now())
                .eventType("RESOLVED")
                .changedBy(userId)
                .reason(reason)
                .build();
    }
}

