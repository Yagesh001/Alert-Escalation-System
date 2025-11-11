package com.movesync.alert.repository;

import com.movesync.alert.domain.model.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AlertHistory entity
 * Provides audit trail for alert state transitions
 */
@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, String> {

    /**
     * Find all history entries for a specific alert
     * Ordered chronologically for timeline view
     */
    List<AlertHistory> findByAlertIdOrderByTimestampAsc(String alertId);

    /**
     * Find recent history entries
     * Used for dashboard activity stream
     */
    List<AlertHistory> findTop100ByOrderByTimestampDesc();

    /**
     * Find history by event type and time range
     */
    List<AlertHistory> findByEventTypeAndTimestampBetween(
        String eventType,
        LocalDateTime from,
        LocalDateTime to
    );

    /**
     * Count events by type for analytics
     */
    @Query("SELECT h.eventType, COUNT(h) FROM AlertHistory h " +
           "WHERE h.timestamp >= :fromTime " +
           "GROUP BY h.eventType")
    List<Object[]> countEventsByType(@Param("fromTime") LocalDateTime fromTime);

    /**
     * Find recent alert lifecycle events
     */
    @Query("SELECT h FROM AlertHistory h " +
           "WHERE h.timestamp >= :fromTime " +
           "ORDER BY h.timestamp DESC")
    List<AlertHistory> findRecentEvents(@Param("fromTime") LocalDateTime fromTime);

    /**
     * Delete old history entries (for data retention)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM AlertHistory h WHERE h.timestamp < :threshold")
    void deleteOldHistory(@Param("threshold") LocalDateTime threshold);
}

