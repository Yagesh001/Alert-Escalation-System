package com.movesync.alert.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration using Caffeine
 * 
 * Caching Strategy:
 * - alerts: TTL 10 minutes, max 1000 entries
 * - rules: TTL 5 minutes, max 100 entries
 * - dashboard: TTL 5 minutes, max 50 entries
 * - drivers: TTL 10 minutes, max 500 entries
 * 
 * Benefits:
 * - Reduces database load
 * - Improves response time
 * - In-memory, low latency
 * 
 * Trade-offs:
 * - May serve slightly stale data (up to TTL)
 * - Memory usage (controlled by max size)
 * - Cache invalidation on updates
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager with custom specifications
     */
    @Bean
    @Primary
    @SuppressWarnings("null")
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "alerts", "rules", "dashboard", "drivers"
        );
        
        cacheManager.setCaffeine(caffeineCacheBuilder());
        
        return cacheManager;
    }

    /**
     * Caffeine cache builder with default configuration
     */
    @SuppressWarnings("null")
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats(); // Enable statistics for monitoring
    }

    /**
     * Separate cache for rules (shorter TTL)
     */
    @Bean("rulesCacheManager")
    @SuppressWarnings("null")
    public CacheManager rulesCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("rules");
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        
        return cacheManager;
    }

    /**
     * Dashboard cache (shorter TTL for real-time feel)
     */
    @Bean("dashboardCacheManager")
    @SuppressWarnings("null")
    public CacheManager dashboardCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("dashboard");
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        
        return cacheManager;
    }
}

