package com.movesync.alert;

import com.movesync.alert.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Main application class for Intelligent Alert Escalation & Resolution System
 * 
 * System Features:
 * - Centralized alert management from multiple sources
 * - Configurable rule engine with DSL support
 * - Automatic escalation based on dynamic rules
 * - Background job for auto-closure
 * - Dashboard with real-time analytics
 * - JWT-based authentication
 * - Comprehensive monitoring and caching
 * 
 * @author MoveInSync
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class IntelligentAlertSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelligentAlertSystemApplication.class, args);
        log.info("Intelligent Alert Escalation & Resolution System started successfully");
    }

    /**
     * Initialize default users on startup
     */
    @Bean
    CommandLineRunner initUsers(AuthService authService) {
        return args -> {
            try {
                // Create default admin user
                authService.registerUser(
                    "admin",
                    "admin@moveinsync.com",
                    "admin123",
                    "ADMIN", "USER"
                );
                log.info("Default admin user created: username=admin, password=admin123");

                // Create default operator user
                authService.registerUser(
                    "operator",
                    "operator@moveinsync.com",
                    "operator123",
                    "USER"
                );
                log.info("Default operator user created: username=operator, password=operator123");

            } catch (Exception e) {
                log.info("Default users already exist or error creating them: {}", e.getMessage());
            }
        };
    }
}

