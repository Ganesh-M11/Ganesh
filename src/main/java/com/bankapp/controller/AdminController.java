package com.bankapp.controller;

import com.bankapp.entity.SupportTicket;
import com.bankapp.entity.Transaction;
import com.bankapp.entity.User;
import com.bankapp.security.CustomUserDetailsService;
import com.bankapp.service.NotificationService;
import com.bankapp.service.SupportTicketService;
import com.bankapp.service.TransactionService;
import com.bankapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private SupportTicketService supportTicketService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public String adminDashboard(Model model) {
        // System statistics
        long totalUsers = userService.getTotalUserCount();
        long activeUsers = userService.getActiveUserCount();
        long openTickets = supportTicketService.getTicketCountByStatus(SupportTicket.TicketStatus.OPEN);
        long pendingTransactions = transactionService.getPendingTransactions().size();

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("openTickets", openTickets);
        model.addAttribute("pendingTransactions", pendingTransactions);

        // Recent activities
        Pageable recentPageable = PageRequest.of(0, 5);
        Page<User> recentUsers = userService.getAllUsers(recentPageable);
        Page<Transaction> recentTransactions = transactionService.getAllTransactions(recentPageable);
        List<SupportTicket> activeTickets = supportTicketService.getActiveTickets();

        model.addAttribute("recentUsers", recentUsers.getContent());
        model.addAttribute("recentTransactions", recentTransactions.getContent());
        model.addAttribute("activeTickets", activeTickets.size() > 5 ? activeTickets.subList(0, 5) : activeTickets);

        // Chart data
        List<Object[]> monthlyTransactions = transactionService.getMonthlyTransactionSummary();
        List<Object[]> ticketsByCategory = supportTicketService.getTicketCountByCategory();

        model.addAttribute("monthlyTransactions", monthlyTransactions);
        model.addAttribute("ticketsByCategory", ticketsByCategory);

        return "admin/dashboard";
    }

    // User Management
    @GetMapping("/users")
    public String manageUsers(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users;

        if (search != null && !search.isEmpty()) {
            users = userService.searchUsers(search, pageable);
        } else {
            users = userService.getAllUsers(pageable);
        }

        model.addAttribute("users", users);
        model.addAttribute("searchTerm", search);

        return "admin/users";
    }

    @GetMapping("/users/{id}")
    public String viewUserDetails(@PathVariable Long id, Model model) {
        try {
            User user = userService.getUserById(id);
            model.addAttribute("user", user);

            // Get user's recent transactions
            Pageable transactionPageable = PageRequest.of(0, 10);
            Page<Transaction> userTransactions = transactionService.getUserTransactions(user, transactionPageable);
            model.addAttribute("userTransactions", userTransactions.getContent());

            // Get user's support tickets
            Page<SupportTicket> userTickets = supportTicketService.getUserTickets(user, transactionPageable);
            model.addAttribute("userTickets", userTickets.getContent());

            return "admin/user-details";
        } catch (RuntimeException e) {
            return "redirect:/admin/users?error=usernotfound";
        }
    }

    @PostMapping("/users/{id}/activate")
    public String activateUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.activateUser(id);
            redirectAttributes.addFlashAttribute("success", "User activated successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deactivateUser(id);
            redirectAttributes.addFlashAttribute("success", "User deactivated successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    // Transaction Management
    @GetMapping("/transactions")
    public String manageTransactions(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions;

        if (search != null && !search.isEmpty()) {
            transactions = transactionService.searchTransactions(search, pageable);
        } else {
            transactions = transactionService.getAllTransactions(pageable);
        }

        model.addAttribute("transactions", transactions);
        model.addAttribute("searchTerm", search);

        // Large transactions for monitoring
        List<Transaction> largeTransactions = transactionService.getLargeTransactions();
        model.addAttribute("largeTransactions", largeTransactions.size() > 10 ? largeTransactions.subList(0, 10) : largeTransactions);

        return "admin/transactions";
    }

    @GetMapping("/transactions/{id}")
    public String viewTransactionDetails(@PathVariable Long id, Model model) {
        try {
            Transaction transaction = transactionService.getTransactionById(id);
            model.addAttribute("transaction", transaction);
            return "admin/transaction-details";
        } catch (RuntimeException e) {
            return "redirect:/admin/transactions?error=transactionnotfound";
        }
    }

    // Support Ticket Management
    @GetMapping("/support")
    public String manageSupport(Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SupportTicket> tickets;

        if (search != null && !search.isEmpty()) {
            tickets = supportTicketService.searchTickets(search, pageable);
        } else if (status != null && !status.isEmpty()) {
            SupportTicket.TicketStatus ticketStatus = SupportTicket.TicketStatus.valueOf(status.toUpperCase());
            tickets = supportTicketService.getTicketsByStatus(ticketStatus, pageable);
        } else {
            tickets = supportTicketService.getAllTickets(pageable);
        }

        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("searchTerm", search);
        model.addAttribute("ticketStatuses", SupportTicket.TicketStatus.values());

        // Statistics
        long openTickets = supportTicketService.getTicketCountByStatus(SupportTicket.TicketStatus.OPEN);
        long inProgressTickets = supportTicketService.getTicketCountByStatus(SupportTicket.TicketStatus.IN_PROGRESS);
        long resolvedTickets = supportTicketService.getTicketCountByStatus(SupportTicket.TicketStatus.RESOLVED);

        model.addAttribute("openTickets", openTickets);
        model.addAttribute("inProgressTickets", inProgressTickets);
        model.addAttribute("resolvedTickets", resolvedTickets);

        return "admin/support";
    }

    @GetMapping("/support/{id}")
    public String viewTicketDetails(@PathVariable Long id, Model model) {
        try {
            SupportTicket ticket = supportTicketService.getTicketById(id);
            model.addAttribute("ticket", ticket);
            model.addAttribute("ticketStatuses", SupportTicket.TicketStatus.values());
            return "admin/ticket-details";
        } catch (RuntimeException e) {
            return "redirect:/admin/support?error=ticketnotfound";
        }
    }

    @PostMapping("/support/{id}/update-status")
    public String updateTicketStatus(@PathVariable Long id,
                                   @RequestParam SupportTicket.TicketStatus status,
                                   RedirectAttributes redirectAttributes) {
        try {
            User currentAdmin = getCurrentUser();
            supportTicketService.updateTicketStatus(id, status, currentAdmin.getFullName());
            redirectAttributes.addFlashAttribute("success", "Ticket status updated successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/support/" + id;
    }

    @PostMapping("/support/{id}/resolve")
    public String resolveTicket(@PathVariable Long id,
                               @RequestParam String resolution,
                               RedirectAttributes redirectAttributes) {
        try {
            User currentAdmin = getCurrentUser();
            supportTicketService.resolveTicket(id, resolution, currentAdmin.getFullName());
            redirectAttributes.addFlashAttribute("success", "Ticket resolved successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/support/" + id;
    }

    // Reports and Analytics
    @GetMapping("/reports")
    public String viewReports(Model model) {
        // User statistics
        long totalUsers = userService.getTotalUserCount();
        long activeUsers = userService.getActiveUserCount();
        long adminUsers = userService.getTotalUserCount(); // This would need a proper method

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("inactiveUsers", totalUsers - activeUsers);

        // Transaction statistics
        List<Object[]> monthlyTransactions = transactionService.getMonthlyTransactionSummary();
        model.addAttribute("monthlyTransactions", monthlyTransactions);

        // Support statistics
        List<Object[]> ticketsByCategory = supportTicketService.getTicketCountByCategory();
        List<Object[]> ticketsByPriority = supportTicketService.getActiveTicketCountByPriority();

        model.addAttribute("ticketsByCategory", ticketsByCategory);
        model.addAttribute("ticketsByPriority", ticketsByPriority);

        return "admin/reports";
    }

    // System Management
    @GetMapping("/system")
    public String systemManagement(Model model) {
        // System health information
        List<User> lockedUsers = userService.getLockedUsers();
        List<User> inactiveUsers = userService.getInactiveUsers(90); // Users inactive for 90 days
        List<Transaction> pendingTransactions = transactionService.getPendingTransactions();

        model.addAttribute("lockedUsers", lockedUsers);
        model.addAttribute("inactiveUsers", inactiveUsers.size() > 10 ? inactiveUsers.subList(0, 10) : inactiveUsers);
        model.addAttribute("pendingTransactions", pendingTransactions.size() > 10 ? pendingTransactions.subList(0, 10) : pendingTransactions);

        return "admin/system";
    }

    @PostMapping("/system/broadcast-notification")
    public String broadcastNotification(@RequestParam String title,
                                      @RequestParam String message,
                                      RedirectAttributes redirectAttributes) {
        try {
            // In a real implementation, you'd broadcast to all active users
            // For now, we'll just show a success message
            redirectAttributes.addFlashAttribute("success", "Notification broadcast initiated");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/system";
    }

    // API endpoints for admin
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<?> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalUsers", userService.getTotalUserCount());
        stats.put("activeUsers", userService.getActiveUserCount());
        stats.put("openTickets", supportTicketService.getTicketCountByStatus(SupportTicket.TicketStatus.OPEN));
        stats.put("pendingTransactions", transactionService.getPendingTransactions().size());
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/api/users/{id}/activate")
    @ResponseBody
    public ResponseEntity<?> apiActivateUser(@PathVariable Long id) {
        try {
            userService.activateUser(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "User activated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/users/{id}/deactivate")
    @ResponseBody
    public ResponseEntity<?> apiDeactivateUser(@PathVariable Long id) {
        try {
            userService.deactivateUser(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "User deactivated successfully"));
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
}