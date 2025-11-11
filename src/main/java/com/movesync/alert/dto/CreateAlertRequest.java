package com.movesync.alert.dto;

import com.movesync.alert.domain.enums.AlertSeverity;
import com.movesync.alert.domain.enums.AlertType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for creating a new alert
 * Used by external modules to ingest alerts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlertRequest {

    @NotNull(message = "Alert type is required")
    private AlertType alertType;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    private String driverId;
    private String vehicleId;
    private String routeId;

    private LocalDateTime timestamp;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    // Helper method to add metadata
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
}

