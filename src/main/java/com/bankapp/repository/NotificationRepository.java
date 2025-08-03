package com.bankapp.repository;

import com.bankapp.entity.Notification;
import com.bankapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, Boolean isRead);

    Page<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, Boolean isRead, Pageable pageable);

    List<Notification> findByUserAndNotificationTypeOrderByCreatedAtDesc(User user, Notification.NotificationType notificationType);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false")
    long countUnreadNotificationsByUser(@Param("user") User user);

    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readTime WHERE n.id = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("readTime") LocalDateTime readTime);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readTime WHERE n.user = :user AND n.isRead = false")
    void markAllAsReadForUser(@Param("user") User user, @Param("readTime") LocalDateTime readTime);

    @Query("SELECT n FROM Notification n WHERE n.createdAt < :cutoffDate")
    List<Notification> findOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}