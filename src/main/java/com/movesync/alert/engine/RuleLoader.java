package com.movesync.alert.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movesync.alert.domain.model.EscalationRule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads and manages escalation rules from JSON configuration file
 * Supports hot-reload of rules without system restart
 * 
 * Design Pattern: Strategy Pattern - Rules are loaded and can be swapped dynamically
 * Thread Safety: Synchronized loading to prevent concurrent modification
 * 
 * Space Complexity: O(r) where r = number of rules
 */
@Slf4j
@Component
public class RuleLoader {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    
    @Value("${alert.rules.config-path:classpath:rules.json}")
    private String rulesConfigPath;

    @Getter
    private List<EscalationRule> rules = new ArrayList<>();

    public RuleLoader(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    /**
     * Load rules on application startup
     */
    @PostConstruct
    public void init() {
        loadRules();
    }

    /**
     * Load rules from configuration file
     * Synchronized to prevent concurrent loading
     * Cache is evicted to ensure fresh data
     * 
     * Time Complexity: O(r) where r = number of rules
     */
    @CacheEvict(value = "rules", allEntries = true)
    public synchronized void loadRules() {
        try {
            Resource resource = resourceLoader.getResource(rulesConfigPath);
            
            if (!resource.exists()) {
                log.error("Rules configuration file not found: {}", rulesConfigPath);
                throw new IllegalStateException("Rules configuration file not found: " + rulesConfigPath);
            }

            try (InputStream inputStream = resource.getInputStream()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = objectMapper.readValue(inputStream, Map.class);
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rulesData = (List<Map<String, Object>>) config.get("rules");
                
                if (rulesData == null || rulesData.isEmpty()) {
                    log.warn("No rules found in configuration file");
                    this.rules = new ArrayList<>();
                    return;
                }

                List<EscalationRule> loadedRules = new ArrayList<>();
                
                for (Map<String, Object> ruleData : rulesData) {
                    EscalationRule rule = parseRule(ruleData);
                    loadedRules.add(rule);
                }

                this.rules = loadedRules;
                log.info("Successfully loaded {} rules from {}", rules.size(), rulesConfigPath);
                
                // Log loaded rules for debugging
                rules.forEach(rule -> log.debug("Loaded rule: {} - escalate if {} occurrences in {} minutes",
                    rule.getAlertType(), rule.getEscalateIfCount(), rule.getWindowMinutes()));

            }
        } catch (IOException e) {
            log.error("Failed to load rules from configuration file: {}", rulesConfigPath, e);
            throw new IllegalStateException("Failed to load rules configuration", e);
        }
    }

    /**
     * Parse a single rule from JSON data
     * Handles DSL-like syntax from configuration
     */
    private EscalationRule parseRule(Map<String, Object> ruleData) {
        EscalationRule.EscalationRuleBuilder builder = EscalationRule.builder();

        // Parse alert type
        String alertTypeStr = (String) ruleData.get("alertType");
        if (alertTypeStr != null) {
            try {
                builder.alertType(com.movesync.alert.domain.enums.AlertType.valueOf(alertTypeStr));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid alert type: {}", alertTypeStr);
            }
        }

        // Parse escalation criteria
        if (ruleData.containsKey("escalateIfCount")) {
            builder.escalateIfCount(((Number) ruleData.get("escalateIfCount")).intValue());
        }

        if (ruleData.containsKey("windowMinutes")) {
            builder.windowMinutes(((Number) ruleData.get("windowMinutes")).intValue());
        }

        // Parse escalation severity
        String severityStr = (String) ruleData.get("escalationSeverity");
        if (severityStr != null) {
            try {
                builder.escalationSeverity(
                    com.movesync.alert.domain.enums.AlertSeverity.valueOf(severityStr)
                );
            } catch (IllegalArgumentException e) {
                log.warn("Invalid severity: {}", severityStr);
                builder.escalationSeverity(com.movesync.alert.domain.enums.AlertSeverity.WARNING);
            }
        }

        // Parse auto-close criteria
        if (ruleData.containsKey("autoCloseIfNoRepeat")) {
            builder.autoCloseIfNoRepeat((Boolean) ruleData.get("autoCloseIfNoRepeat"));
        }

        if (ruleData.containsKey("autoCloseIf")) {
            builder.autoCloseIf((String) ruleData.get("autoCloseIf"));
        }

        if (ruleData.containsKey("autoCloseWindowMinutes")) {
            builder.autoCloseWindowMinutes(((Number) ruleData.get("autoCloseWindowMinutes")).intValue());
        }

        // Parse additional configuration
        builder.enabled((Boolean) ruleData.getOrDefault("enabled", true));
        
        if (ruleData.containsKey("priority")) {
            builder.priority(((Number) ruleData.get("priority")).intValue());
        }

        return builder.build();
    }

    /**
     * Get number of loaded rules
     */
    public int getRuleCount() {
        return rules.size();
    }

    /**
     * Check if rules are loaded
     */
    public boolean isLoaded() {
        return rules != null && !rules.isEmpty();
    }
}

