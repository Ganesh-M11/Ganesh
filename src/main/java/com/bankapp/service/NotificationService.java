package com.bankapp.service;

import com.bankapp.entity.Notification;
import com.bankapp.entity.User;
import com.bankapp.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public Notification createNotification(User user, String title, String message, Notification.NotificationType type) {
        Notification notification = new Notification(title, message, type, user);
        return notificationRepository.save(notification);
    }

    public void createWelcomeNotification(User user) {
        String title = "Welcome to Secure Bank!";
        String message = String.format("Hello %s, welcome to Secure Bank! Your account has been successfully created. " +
                "Your account number is %s. Start exploring our banking services today!",
                user.getFullName(), user.getAccountNumber());
        createNotification(user, title, message, Notification.NotificationType.ACCOUNT_UPDATE);
    }

    public void createTransactionAlert(User user, String transactionType, BigDecimal amount, String description) {
        String title = "Transaction Alert";
        String message = String.format("A %s of $%.2f has been processed on your account. %s",
                transactionType, amount, description);
        createNotification(user, title, message, Notification.NotificationType.TRANSACTION_ALERT);
    }

    public void createLowBalanceAlert(User user, BigDecimal currentBalance) {
        String title = "Low Balance Alert";
        String message = String.format("Your account balance is low: $%.2f. Consider adding funds to avoid any service interruptions.",
                currentBalance);
        createNotification(user, title, message, Notification.NotificationType.LOW_BALANCE);
    }

    public void createSecurityAlert(User user, String message) {
        String title = "Security Alert";
        createNotification(user, title, message, Notification.NotificationType.SECURITY_ALERT);
    }

    public void createAccountUpdate(User user, String message) {
        String title = "Account Update";
        createNotification(user, title, message, Notification.NotificationType.ACCOUNT_UPDATE);
    }

    public void createSystemNotification(User user, String title, String message) {
        createNotification(user, title, message, Notification.NotificationType.SYSTEM_NOTIFICATION);
    }

    public void createPromotionalNotification(User user, String title, String message) {
        createNotification(user, title, message, Notification.NotificationType.PROMOTIONAL);
    }

    // Broadcast notifications to all active users
    public void broadcastNotification(String title, String message, Notification.NotificationType type) {
        // This would typically be done with a batch job or async processing
        // For now, we'll keep it simple
        // In production, you'd want to use Spring Batch or similar for large user bases
    }

    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Page<Notification> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
    }

    public Page<Notification> getUnreadNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false, pageable);
    }

    public long getUnreadNotificationCount(User user) {
        return notificationRepository.countUnreadNotificationsByUser(user);
    }

    public void markNotificationAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId, LocalDateTime.now());
    }

    public void markAllNotificationsAsRead(User user) {
        notificationRepository.markAllAsReadForUser(user, LocalDateTime.now());
    }

    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    public List<Notification> getNotificationsByType(User user, Notification.NotificationType type) {
        return notificationRepository.findByUserAndNotificationTypeOrderByCreatedAtDesc(user, type);
    }

    public List<Notification> getNotificationsByDateRange(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return notificationRepository.findByUserAndDateRange(user, startDate, endDate);
    }

    // Cleanup old notifications (typically run as a scheduled job)
    public void cleanupOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        notificationRepository.deleteByCreatedAtBefore(cutoffDate);
    }
}