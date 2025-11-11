package com.movesync.alert.monitoring;

import com.movesync.alert.engine.RuleLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the alert system
 * Provides health status for monitoring
 */
@Component("alertSystemHealth")
@RequiredArgsConstructor
public class AlertSystemHealthIndicator implements org.springframework.boot.actuate.health.HealthIndicator {

    private final RuleLoader ruleLoader;

    @Override
    public Health health() {
        try {
            // Check if rules are loaded
            if (!ruleLoader.isLoaded()) {
                return Health.down()
                        .withDetail("reason", "Rules not loaded")
                        .build();
            }

            int ruleCount = ruleLoader.getRuleCount();
            
            return Health.up()
                    .withDetail("rulesLoaded", ruleCount)
                    .withDetail("status", "Operational")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

