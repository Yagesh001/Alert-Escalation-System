package com.movesync.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for trend data over time
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataResponse {
    private String date;
    private Map<String, Long> eventCounts; // event type -> count
}

