package com.movesync.alert.dto;

import com.movesync.alert.domain.enums.AlertSeverity;
import com.movesync.alert.domain.enums.AlertStatus;
import com.movesync.alert.domain.enums.AlertType;
import com.movesync.alert.domain.model.Alert;
import com.movesync.alert.domain.model.AlertHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for alert response
 * Returned by API endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    private String alertId;
    private AlertType alertType;
    private AlertSeverity severity;
    private AlertStatus status;
    private LocalDateTime timestamp;
    
    private String driverId;
    private String vehicleId;
    private String routeId;
    
    private Map<String, Object> metadata;
    
    private LocalDateTime escalatedAt;
    private String escalationReason;
    
    private LocalDateTime closedAt;
    private String closureReason;
    private String closedBy;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Optional: Include history for drill-down
    private List<AlertHistory> history;

    /**
     * Factory method to convert Alert entity to response DTO
     */
    public static AlertResponse fromEntity(Alert alert) {
        return AlertResponse.builder()
            .alertId(alert.getAlertId())
            .alertType(alert.getAlertType())
            .severity(alert.getSeverity())
            .status(alert.getStatus())
            .timestamp(alert.getTimestamp())
            .driverId(alert.getDriverId())
            .vehicleId(alert.getVehicleId())
            .routeId(alert.getRouteId())
            .metadata(alert.getMetadata())
            .escalatedAt(alert.getEscalatedAt())
            .escalationReason(alert.getEscalationReason())
            .closedAt(alert.getClosedAt())
            .closureReason(alert.getClosureReason())
            .closedBy(alert.getClosedBy())
            .createdAt(alert.getCreatedAt())
            .updatedAt(alert.getUpdatedAt())
            .build();
    }
}

