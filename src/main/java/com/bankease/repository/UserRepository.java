package com.bankease.repository;

import com.bankease.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username for authentication
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
     * Find users by role
     */
    List<User> findByRole(User.Role role);
    
    /**
     * Find enabled users
     */
    List<User> findByEnabledTrue();
    
    /**
     * Find locked users
     */
    @Query("SELECT u FROM User u WHERE u.accountNonLocked = false OR u.lockTime IS NOT NULL")
    List<User> findLockedUsers();
    
    /**
     * Update failed login attempts
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts WHERE u.username = :username")
    void updateFailedAttempts(@Param("username") String username, @Param("attempts") int attempts);
    
    /**
     * Lock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = false, u.lockTime = :lockTime WHERE u.username = :username")
    void lockUser(@Param("username") String username, @Param("lockTime") LocalDateTime lockTime);
    
    /**
     * Unlock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = true, u.lockTime = NULL, u.failedLoginAttempts = 0 WHERE u.username = :username")
    void unlockUser(@Param("username") String username);
    
    /**
     * Find users created between dates
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findUsersCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count users by role
     */
    long countByRole(User.Role role);
    
    /**
     * Find users with pending account applications
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.bankAccounts ba WHERE ba.status = 'PENDING'")
    List<User> findUsersWithPendingAccounts();
}