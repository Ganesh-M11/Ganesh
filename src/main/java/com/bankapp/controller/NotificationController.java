package com.bankapp.controller;

import com.bankapp.entity.Notification;
import com.bankapp.entity.User;
import com.bankapp.security.CustomUserDetailsService;
import com.bankapp.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public String viewNotifications(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) Boolean unreadOnly) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);

        Page<Notification> notifications;
        if (unreadOnly != null && unreadOnly) {
            notifications = notificationService.getUnreadNotifications(currentUser, pageable);
        } else {
            notifications = notificationService.getUserNotifications(currentUser, pageable);
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("notifications", notifications);
        model.addAttribute("selectedType", type);
        model.addAttribute("unreadOnly", unreadOnly);
        model.addAttribute("notificationTypes", Notification.NotificationType.values());

        // Get unread count
        long unreadCount = notificationService.getUnreadNotificationCount(currentUser);
        model.addAttribute("unreadCount", unreadCount);

        return "notifications/index";
    }

    @GetMapping("/{id}")
    public String viewNotificationDetails(@PathVariable Long id, Model model) {
        try {
            Notification notification = notificationService.getNotificationById(id);
            User currentUser = getCurrentUser();

            // Ensure user can only view their own notifications
            if (!notification.getUser().getId().equals(currentUser.getId())) {
                return "redirect:/notifications?error=unauthorized";
            }

            // Mark as read if not already read
            if (!notification.getIsRead()) {
                notificationService.markNotificationAsRead(id);
                notification.setIsRead(true); // Update the object for display
            }

            model.addAttribute("notification", notification);
            model.addAttribute("user", currentUser);
            return "notifications/details";
        } catch (RuntimeException e) {
            return "redirect:/notifications?error=notfound";
        }
    }

    @PostMapping("/{id}/mark-read")
    public String markAsRead(@PathVariable Long id) {
        try {
            Notification notification = notificationService.getNotificationById(id);
            User currentUser = getCurrentUser();

            // Ensure user can only mark their own notifications as read
            if (!notification.getUser().getId().equals(currentUser.getId())) {
                return "redirect:/notifications?error=unauthorized";
            }

            notificationService.markNotificationAsRead(id);
            return "redirect:/notifications";
        } catch (RuntimeException e) {
            return "redirect:/notifications?error=notfound";
        }
    }

    @PostMapping("/mark-all-read")
    public String markAllAsRead() {
        User currentUser = getCurrentUser();
        notificationService.markAllNotificationsAsRead(currentUser);
        return "redirect:/notifications";
    }

    @PostMapping("/{id}/delete")
    public String deleteNotification(@PathVariable Long id) {
        try {
            Notification notification = notificationService.getNotificationById(id);
            User currentUser = getCurrentUser();

            // Ensure user can only delete their own notifications
            if (!notification.getUser().getId().equals(currentUser.getId())) {
                return "redirect:/notifications?error=unauthorized";
            }

            notificationService.deleteNotification(id);
            return "redirect:/notifications";
        } catch (RuntimeException e) {
            return "redirect:/notifications?error=notfound";
        }
    }

    // API endpoints
    @GetMapping("/api/notifications")
    @ResponseBody
    public ResponseEntity<?> apiGetNotifications(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) Boolean unreadOnly) {
        try {
            User currentUser = getCurrentUser();
            Pageable pageable = PageRequest.of(page, size);

            Page<Notification> notifications;
            if (unreadOnly != null && unreadOnly) {
                notifications = notificationService.getUnreadNotifications(currentUser, pageable);
            } else {
                notifications = notificationService.getUserNotifications(currentUser, pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notifications.getContent().stream()
                .map(this::createNotificationResponse).toList());
            response.put("totalElements", notifications.getTotalElements());
            response.put("totalPages", notifications.getTotalPages());
            response.put("currentPage", page);
            response.put("unreadCount", notificationService.getUnreadNotificationCount(currentUser));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/notifications/unread-count")
    @ResponseBody
    public ResponseEntity<?> apiGetUnreadCount() {
        try {
            User currentUser = getCurrentUser();
            long unreadCount = notificationService.getUnreadNotificationCount(currentUser);

            return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/notifications/{id}/mark-read")
    @ResponseBody
    public ResponseEntity<?> apiMarkAsRead(@PathVariable Long id) {
        try {
            Notification notification = notificationService.getNotificationById(id);
            User currentUser = getCurrentUser();

            // Ensure user can only mark their own notifications as read
            if (!notification.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unauthorized"));
            }

            notificationService.markNotificationAsRead(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification marked as read");
            response.put("unreadCount", notificationService.getUnreadNotificationCount(currentUser));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/notifications/mark-all-read")
    @ResponseBody
    public ResponseEntity<?> apiMarkAllAsRead() {
        try {
            User currentUser = getCurrentUser();
            notificationService.markAllNotificationsAsRead(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All notifications marked as read");
            response.put("unreadCount", 0);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/notifications/{id}")
    @ResponseBody
    public ResponseEntity<?> apiDeleteNotification(@PathVariable Long id) {
        try {
            Notification notification = notificationService.getNotificationById(id);
            User currentUser = getCurrentUser();

            // Ensure user can only delete their own notifications
            if (!notification.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unauthorized"));
            }

            notificationService.deleteNotification(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification deleted successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetailsService.CustomUserPrincipal userPrincipal =
            (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
        return userPrincipal.getUser();
    }

    private Map<String, Object> createNotificationResponse(Notification notification) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", notification.getId());
        response.put("title", notification.getTitle());
        response.put("message", notification.getMessage());
        response.put("type", notification.getNotificationType().name());
        response.put("isRead", notification.getIsRead());
        response.put("createdAt", notification.getCreatedAt());
        response.put("readAt", notification.getReadAt());

        return response;
    }
}