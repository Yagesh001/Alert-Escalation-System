package com.movesync.alert.scheduler;

import com.movesync.alert.engine.RuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for periodic rule reloading
 * Allows hot-reload of rules without system restart
 * 
 * Runs: Every 5 minutes (configurable)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "alert.rules.auto-reload",
    havingValue = "true",
    matchIfMissing = false
)
public class RuleReloadScheduler {

    private final RuleEngine ruleEngine;

    /**
     * Reload rules periodically
     * Allows dynamic rule updates
     */
    @Scheduled(fixedDelayString = "${alert.rules.reload-interval:300000}")
    public void reloadRules() {
        log.debug("Reloading escalation rules");
        
        try {
            ruleEngine.reloadRules();
            log.info("Rules reloaded successfully");
        } catch (Exception e) {
            log.error("Error reloading rules", e);
        }
    }
}

