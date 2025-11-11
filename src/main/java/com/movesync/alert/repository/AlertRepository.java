package com.movesync.alert.repository;

import com.movesync.alert.domain.enums.AlertSeverity;
import com.movesync.alert.domain.enums.AlertStatus;
import com.movesync.alert.domain.enums.AlertType;
import com.movesync.alert.domain.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Alert entity
 * Provides data access methods with optimal indexing for performance
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {

    /**
     * Find all active (OPEN or ESCALATED) alerts
     * Time Complexity: O(n) where n = number of active alerts
     * Uses index on status column
     */
    List<Alert> findByStatusIn(List<AlertStatus> statuses);

    /**
     * Find alerts by status
     * Time Complexity: O(n) where n = number of alerts with that status
     * Uses index on status column
     */
    List<Alert> findByStatus(AlertStatus status);

    /**
     * Find alerts by driver ID and status
     * Time Complexity: O(log n) due to composite index
     */
    List<Alert> findByDriverIdAndStatusIn(String driverId, List<AlertStatus> statuses);

    /**
     * Find alerts by type, driver, and time window
     * Used for rule evaluation to check repeat occurrences
     * Time Complexity: O(log n) with proper indexes
     */
    @Query("SELECT a FROM Alert a WHERE a.alertType = :alertType " +
           "AND a.driverId = :driverId " +
           "AND a.timestamp >= :fromTime " +
           "ORDER BY a.timestamp DESC")
    List<Alert> findRecentAlertsByTypeAndDriver(
        @Param("alertType") AlertType alertType,
        @Param("driverId") String driverId,
        @Param("fromTime") LocalDateTime fromTime
    );

    /**
     * Count alerts by severity and status
     * Used for dashboard statistics
     */
    @Query("SELECT a.severity, COUNT(a) FROM Alert a " +
           "WHERE a.status IN :statuses " +
           "GROUP BY a.severity")
    List<Object[]> countBySeverityAndStatus(@Param("statuses") List<AlertStatus> statuses);

    /**
     * Find top N drivers with most open alerts
     * Used for dashboard "Top Offenders"
     */
    @Query("SELECT a.driverId, COUNT(a) as alertCount FROM Alert a " +
           "WHERE a.status IN :statuses " +
           "AND a.driverId IS NOT NULL " +
           "GROUP BY a.driverId " +
           "ORDER BY alertCount DESC")
    List<Object[]> findTopDriversByAlertCount(@Param("statuses") List<AlertStatus> statuses);

    /**
     * Find recently auto-closed alerts
     * Used for dashboard transparency
     */
    @Query("SELECT a FROM Alert a " +
           "WHERE a.status = 'AUTO_CLOSED' " +
           "AND a.closedAt >= :fromTime " +
           "ORDER BY a.closedAt DESC")
    List<Alert> findRecentlyAutoClosedAlerts(@Param("fromTime") LocalDateTime fromTime);

    /**
     * Find alerts eligible for auto-closure
     * (OPEN or ESCALATED alerts older than threshold)
     */
    @Query("SELECT a FROM Alert a " +
           "WHERE a.status IN ('OPEN', 'ESCALATED') " +
           "AND a.timestamp < :threshold " +
           "ORDER BY a.timestamp ASC")
    List<Alert> findAlertsEligibleForAutoClosure(@Param("threshold") LocalDateTime threshold);

    /**
     * Find escalated alerts for monitoring
     */
    List<Alert> findByStatusAndSeverity(AlertStatus status, AlertSeverity severity);

    /**
     * Count alerts by status
     */
    long countByStatus(AlertStatus status);

    /**
     * Count alerts by type and time window
     */
    long countByAlertTypeAndTimestampAfter(AlertType alertType, LocalDateTime after);

    /**
     * Delete old alerts (for data retention)
     * Space Optimization: Remove alerts older than retention period
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Alert a WHERE a.timestamp < :threshold")
    void deleteOldAlerts(@Param("threshold") LocalDateTime threshold);

    /**
     * Find alerts by multiple criteria (for advanced filtering)
     */
    @Query("SELECT a FROM Alert a WHERE " +
           "(:alertType IS NULL OR a.alertType = :alertType) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:driverId IS NULL OR a.driverId = :driverId) AND " +
           "(:fromDate IS NULL OR a.timestamp >= :fromDate) AND " +
           "(:toDate IS NULL OR a.timestamp <= :toDate) " +
           "ORDER BY a.timestamp DESC")
    List<Alert> findAlertsByFilters(
        @Param("alertType") AlertType alertType,
        @Param("severity") AlertSeverity severity,
        @Param("status") AlertStatus status,
        @Param("driverId") String driverId,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate
    );
}

