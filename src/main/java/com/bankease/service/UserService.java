package com.bankease.service;

import com.bankease.model.User;
import com.bankease.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_TIME_DURATION = 30; // 30 minutes

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        return user;
    }

    /**
     * Register a new user
     */
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Set default role
        user.setRole(User.Role.ROLE_USER);
        
        return userRepository.save(user);
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get users by role
     */
    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    /**
     * Update user profile
     */
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Update user password
     */
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Enable/Disable user account
     */
    public void toggleUserAccount(Long userId, boolean enabled) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setEnabled(enabled);
            userRepository.save(user);
        }
    }

    /**
     * Handle failed login attempts
     */
    public void handleFailedLogin(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountNonLocked(false);
                user.setLockTime(LocalDateTime.now());
                userRepository.updateFailedAttempts(username, failedAttempts);
                userRepository.lockUser(username, LocalDateTime.now());
            } else {
                userRepository.updateFailedAttempts(username, failedAttempts);
            }
        }
    }

    /**
     * Reset failed login attempts on successful login
     */
    public void resetFailedAttempts(String username) {
        userRepository.updateFailedAttempts(username, 0);
    }

    /**
     * Unlock user account
     */
    public void unlockUser(String username) {
        userRepository.unlockUser(username);
    }

    /**
     * Check if user account is locked and should be unlocked
     */
    public boolean unlockWhenTimeExpired(User user) {
        long lockTimeInMillis = user.getLockTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long currentTimeInMillis = System.currentTimeMillis();
        
        if (lockTimeInMillis + (LOCK_TIME_DURATION * 60 * 1000) < currentTimeInMillis) {
            user.setAccountNonLocked(true);
            user.setLockTime(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            return true;
        }
        
        return false;
    }

    /**
     * Create admin user if not exists
     */
    public void createAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@bankease.com");
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setPhoneNumber("+1234567890");
            admin.setAddress("System Address");
            admin.setRole(User.Role.ROLE_ADMIN);
            admin.setEnabled(true);
            
            userRepository.save(admin);
        }
    }

    /**
     * Get user statistics
     */
    public UserStats getUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByRole(User.Role.ROLE_USER);
        long adminUsers = userRepository.countByRole(User.Role.ROLE_ADMIN);
        List<User> lockedUsers = userRepository.findLockedUsers();
        
        return new UserStats(totalUsers, activeUsers, adminUsers, lockedUsers.size());
    }

    /**
     * Delete user (soft delete by disabling)
     */
    public void deleteUser(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setEnabled(false);
            userRepository.save(user);
        }
    }

    /**
     * Check if username is available
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Check if email is available
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    // Inner class for user statistics
    public static class UserStats {
        private long totalUsers;
        private long activeUsers;
        private long adminUsers;
        private long lockedUsers;

        public UserStats(long totalUsers, long activeUsers, long adminUsers, long lockedUsers) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.adminUsers = adminUsers;
            this.lockedUsers = lockedUsers;
        }

        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getAdminUsers() { return adminUsers; }
        public long getLockedUsers() { return lockedUsers; }
    }
}