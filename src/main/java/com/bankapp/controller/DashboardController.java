package com.bankapp.controller;

import com.bankapp.entity.Transaction;
import com.bankapp.entity.User;
import com.bankapp.security.CustomUserDetailsService;
import com.bankapp.service.NotificationService;
import com.bankapp.service.TransactionService;
import com.bankapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public String dashboard(Model model) {
        User currentUser = getCurrentUser();
        
        // Basic account information
        model.addAttribute("user", currentUser);
        model.addAttribute("accountBalance", currentUser.getAccountBalance());
        model.addAttribute("accountNumber", currentUser.getAccountNumber());
        
        // Recent transactions (last 5)
        Pageable recentTransactionsPageable = PageRequest.of(0, 5);
        Page<Transaction> recentTransactions = transactionService.getUserTransactions(currentUser, recentTransactionsPageable);
        model.addAttribute("recentTransactions", recentTransactions.getContent());
        
        // Transaction statistics
        BigDecimal totalDeposits = transactionService.getTotalAmountByUserAndType(currentUser, Transaction.TransactionType.DEPOSIT);
        BigDecimal totalWithdrawals = transactionService.getTotalAmountByUserAndType(currentUser, Transaction.TransactionType.WITHDRAWAL);
        BigDecimal totalTransfersOut = transactionService.getTotalAmountByUserAndType(currentUser, Transaction.TransactionType.TRANSFER_OUT);
        BigDecimal totalTransfersIn = transactionService.getTotalAmountByUserAndType(currentUser, Transaction.TransactionType.TRANSFER_IN);
        
        model.addAttribute("totalDeposits", totalDeposits);
        model.addAttribute("totalWithdrawals", totalWithdrawals);
        model.addAttribute("totalTransfersOut", totalTransfersOut);
        model.addAttribute("totalTransfersIn", totalTransfersIn);
        
        // Transaction count
        long transactionCount = transactionService.getUserTransactionCount(currentUser);
        model.addAttribute("transactionCount", transactionCount);
        
        // Unread notifications count
        long unreadNotifications = notificationService.getUnreadNotificationCount(currentUser);
        model.addAttribute("unreadNotifications", unreadNotifications);
        
        // Daily transfer limit information
        BigDecimal dailyTransferred = transactionService.getDailyTransferAmount(currentUser.getId());
        BigDecimal dailyLimit = new BigDecimal("10000.00"); // Should match TransactionService constant
        model.addAttribute("dailyTransferred", dailyTransferred);
        model.addAttribute("remainingDailyLimit", dailyLimit.subtract(dailyTransferred));
        
        // Account status indicators
        model.addAttribute("isAccountLocked", currentUser.isAccountLocked());
        model.addAttribute("isLowBalance", currentUser.getAccountBalance().compareTo(new BigDecimal("100.00")) < 0);
        
        return "dashboard/index";
    }

    @GetMapping("/account-summary")
    public String accountSummary(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size) {
        User currentUser = getCurrentUser();
        
        model.addAttribute("user", currentUser);
        
        // Paginated transactions
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.getUserTransactions(currentUser, pageable);
        model.addAttribute("transactions", transactions);
        
        // Monthly summary for the current year
        LocalDateTime startOfYear = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfYear = startOfYear.plusYears(1).minusSeconds(1);
        List<Object[]> monthlySummary = transactionService.getDailyTransactionSummaryForUser(currentUser, startOfYear, endOfYear);
        model.addAttribute("monthlySummary", monthlySummary);
        
        return "dashboard/account-summary";
    }

    @GetMapping("/quick-actions")
    public String quickActions(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("user", currentUser);
        model.addAttribute("currentBalance", currentUser.getAccountBalance());
        
        // Calculate remaining daily transfer limit
        BigDecimal dailyTransferred = transactionService.getDailyTransferAmount(currentUser.getId());
        BigDecimal dailyLimit = new BigDecimal("10000.00");
        model.addAttribute("remainingDailyLimit", dailyLimit.subtract(dailyTransferred));
        
        return "dashboard/quick-actions";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("user", currentUser);
        
        // Account creation and last login info
        model.addAttribute("accountAge", java.time.Duration.between(currentUser.getCreatedAt(), LocalDateTime.now()).toDays());
        model.addAttribute("lastLogin", currentUser.getLastLogin());
        
        return "dashboard/profile";
    }

    @GetMapping("/security")
    public String security(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("user", currentUser);
        
        // Security information
        model.addAttribute("failedLoginAttempts", currentUser.getFailedLoginAttempts());
        model.addAttribute("isAccountLocked", currentUser.isAccountLocked());
        model.addAttribute("accountLockedUntil", currentUser.getAccountLockedUntil());
        
        return "dashboard/security";
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
            (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
        return userPrincipal.getUser();
    }
}