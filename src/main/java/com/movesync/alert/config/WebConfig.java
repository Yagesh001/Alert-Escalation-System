package com.movesync.alert.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration
 * Properly configures static resources without interfering with API routes
 */
@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            
            @Override
            public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
                // Serve static resources ONLY from /static/** path
                // This ensures /api/** routes go to controllers
                registry.addResourceHandler("/static/**")
                        .addResourceLocations("classpath:/static/")
                        .setCachePeriod(0); // No cache for development
            }
            
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600);
            }
        };
    }
}

