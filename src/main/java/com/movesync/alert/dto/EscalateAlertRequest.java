package com.movesync.alert.dto;

import com.movesync.alert.domain.enums.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for manually escalating an alert
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalateAlertRequest {

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    @NotBlank(message = "Reason is required")
    private String reason;
}

