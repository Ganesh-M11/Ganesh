package com.bankapp.service;

import com.bankapp.entity.User;
import com.bankapp.repository.UserRepository;
import com.bankapp.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private NotificationService notificationService;

    public User registerUser(User user) {
        // Validate email uniqueness
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Generate unique account number
        user.setAccountNumber(generateAccountNumber());
        
        // Set default values
        user.setRole(User.Role.USER);
        user.setIsActive(true);
        user.setIsAccountVerified(true); // In production, this would be false until email verification
        user.setAccountBalance(BigDecimal.ZERO);
        user.setFailedLoginAttempts(0);

        User savedUser = userRepository.save(user);

        // Send welcome notification
        notificationService.createWelcomeNotification(savedUser);

        return savedUser;
    }

    public Optional<User> authenticateUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            // Check if account is locked
            if (user.isAccountLocked()) {
                throw new RuntimeException("Account is temporarily locked. Please try again later.");
            }
            
            // Check if account is active
            if (!user.getIsActive()) {
                throw new RuntimeException("Account is deactivated. Please contact support.");
            }
            
            // Verify password
            if (passwordEncoder.matches(password, user.getPassword())) {
                // Reset failed login attempts and update last login
                user.setFailedLoginAttempts(0);
                user.setAccountLockedUntil(null);
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                
                return Optional.of(user);
            } else {
                // Increment failed login attempts
                handleFailedLogin(user);
                throw new RuntimeException("Invalid credentials");
            }
        }
        
        throw new RuntimeException("User not found");
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        
        // Lock account after 5 failed attempts for 30 minutes
        if (attempts >= 5) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
            notificationService.createSecurityAlert(user, "Account temporarily locked due to multiple failed login attempts");
        }
        
        userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByAccountNumber(String accountNumber) {
        return userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public User updateUser(User user) {
        User existingUser = getUserById(user.getId());
        
        // Update allowed fields
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setPhoneNumber(user.getPhoneNumber());
        
        return userRepository.save(existingUser);
    }

    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        notificationService.createSecurityAlert(user, "Your password has been successfully changed");
    }

    public void updateAccountBalance(Long userId, BigDecimal newBalance) {
        User user = getUserById(userId);
        BigDecimal oldBalance = user.getAccountBalance();
        user.setAccountBalance(newBalance);
        userRepository.save(user);
        
        // Check for low balance alert
        if (newBalance.compareTo(BigDecimal.valueOf(100)) < 0) {
            notificationService.createLowBalanceAlert(user, newBalance);
        }
    }

    public void deactivateUser(Long userId) {
        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);
        
        notificationService.createAccountUpdate(user, "Your account has been deactivated");
    }

    public void activateUser(Long userId) {
        User user = getUserById(userId);
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
        
        notificationService.createAccountUpdate(user, "Your account has been activated");
    }

    // Admin methods
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> getActiveUsers(Pageable pageable) {
        return userRepository.findByIsActiveTrue(pageable);
    }

    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.searchUsers(searchTerm, pageable);
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public long getActiveUserCount() {
        return userRepository.countActiveUsers();
    }

    public List<User> getLockedUsers() {
        return userRepository.findLockedUsers(LocalDateTime.now());
    }

    public List<User> getInactiveUsers(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return userRepository.findInactiveUsers(cutoffDate);
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            // Generate 12-digit account number starting with "ACC"
            accountNumber = "ACC" + String.format("%09d", (int)(Math.random() * 1000000000));
        } while (userRepository.existsByAccountNumber(accountNumber));
        
        return accountNumber;
    }
}