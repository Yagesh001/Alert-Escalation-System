package com.movesync.alert.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for recording custom application metrics
 * Integrates with Micrometer/Prometheus for monitoring
 * 
 * Metrics Categories:
 * - Alert operations (create, escalate, close, resolve)
 * - Rule evaluations
 * - Background job executions
 * - API performance
 */
@Slf4j
@Service
public class AlertMetricsService {

    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Counter alertsCreatedCounter;
    private final Counter alertsEscalatedCounter;
    private final Counter alertsAutoClosedCounter;
    private final Counter alertsResolvedCounter;
    private final Counter ruleEvaluationsCounter;
    
    // Timers
    private final Timer alertCreationTimer;
    private final Timer ruleEvaluationTimer;

    public AlertMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.alertsCreatedCounter = Counter.builder("alerts.created")
                .description("Total number of alerts created")
                .register(meterRegistry);
        
        this.alertsEscalatedCounter = Counter.builder("alerts.escalated")
                .description("Total number of alerts escalated")
                .register(meterRegistry);
        
        this.alertsAutoClosedCounter = Counter.builder("alerts.autoclosed")
                .description("Total number of alerts auto-closed")
                .register(meterRegistry);
        
        this.alertsResolvedCounter = Counter.builder("alerts.resolved")
                .description("Total number of alerts manually resolved")
                .register(meterRegistry);
        
        this.ruleEvaluationsCounter = Counter.builder("rules.evaluations")
                .description("Total number of rule evaluations")
                .register(meterRegistry);
        
        // Initialize timers
        this.alertCreationTimer = Timer.builder("alerts.creation.time")
                .description("Time taken to create an alert")
                .register(meterRegistry);
        
        this.ruleEvaluationTimer = Timer.builder("rules.evaluation.time")
                .description("Time taken to evaluate rules")
                .register(meterRegistry);
    }

    /**
     * Record alert creation
     */
    public void recordAlertCreated() {
        alertsCreatedCounter.increment();
    }

    /**
     * Record alert escalation
     */
    public void recordAlertEscalated() {
        alertsEscalatedCounter.increment();
    }

    /**
     * Record alert auto-closure
     */
    public void recordAlertAutoClosed() {
        alertsAutoClosedCounter.increment();
    }

    /**
     * Record alert resolution
     */
    public void recordAlertResolved() {
        alertsResolvedCounter.increment();
    }

    /**
     * Record rule evaluation
     */
    public void recordRuleEvaluation() {
        ruleEvaluationsCounter.increment();
    }

    /**
     * Record alert creation time
     */
    public void recordAlertCreationTime(long milliseconds) {
        alertCreationTimer.record(milliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * Record rule evaluation time
     */
    public void recordRuleEvaluationTime(long milliseconds) {
        ruleEvaluationTimer.record(milliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * Record custom metric
     */
    public void recordCustomMetric(String metricName, double value) {
        meterRegistry.gauge(metricName, value);
    }
}

