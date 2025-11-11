package com.movesync.alert.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for scheduling background jobs
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Scheduling is enabled via annotation
    // Task execution pool size is configured in application.yml
}

