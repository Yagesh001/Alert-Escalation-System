package com.movesync.alert.repository;

import com.movesync.alert.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity
 * Used for authentication and authorization
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by username (for authentication)
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find enabled users only
     */
    Optional<User> findByUsernameAndEnabledTrue(String username);
}

