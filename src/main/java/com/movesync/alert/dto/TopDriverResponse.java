package com.movesync.alert.dto;

import com.movesync.alert.domain.enums.AlertSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for top driver (offender) information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopDriverResponse {
    private String driverId;
    private Long totalAlerts;
    private Map<AlertSeverity, Long> severityBreakdown;
}

