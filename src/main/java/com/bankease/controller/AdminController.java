package com.bankease.controller;

import com.bankease.model.BankAccount;
import com.bankease.model.Transaction;
import com.bankease.model.User;
import com.bankease.service.AccountService;
import com.bankease.service.TransactionService;
import com.bankease.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    /**
     * Admin dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Get statistics
        UserService.UserStats userStats = userService.getUserStats();
        AccountService.AccountStats accountStats = accountService.getAccountStats();
        TransactionService.TransactionStats transactionStats = transactionService.getTransactionStats();

        // Get recent data
        List<BankAccount> pendingAccounts = accountService.getPendingAccounts();
        List<Transaction> recentTransactions = transactionService.getTodaysTransactions();
        List<BankAccount> recentAccounts = accountService.getRecentAccounts();

        // Limit lists for dashboard
        if (pendingAccounts.size() > 10) {
            pendingAccounts = pendingAccounts.subList(0, 10);
        }
        if (recentTransactions.size() > 10) {
            recentTransactions = recentTransactions.subList(0, 10);
        }
        if (recentAccounts.size() > 10) {
            recentAccounts = recentAccounts.subList(0, 10);
        }

        model.addAttribute("userStats", userStats);
        model.addAttribute("accountStats", accountStats);
        model.addAttribute("transactionStats", transactionStats);
        model.addAttribute("pendingAccounts", pendingAccounts);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("recentAccounts", recentAccounts);

        return "admin/dashboard";
    }

    /**
     * Manage users page
     */
    @GetMapping("/users")
    public String manageUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "admin/manageUsers";
    }

    /**
     * User details page
     */
    @GetMapping("/users/{id}")
    public String userDetails(@PathVariable Long id, Model model) {
        Optional<User> userOptional = userService.findById(id);
        if (userOptional.isEmpty()) {
            return "redirect:/admin/users";
        }

        User user = userOptional.get();
        List<BankAccount> accounts = accountService.getAccountsByUser(user);
        List<Transaction> transactions = transactionService.getTransactionsByUserId(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("accounts", accounts);
        model.addAttribute("transactions", transactions);
        model.addAttribute("transactionCount", transactions.size());

        return "admin/user-details";
    }

    /**
     * Toggle user account status
     */
    @PostMapping("/users/{id}/toggle")
    public String toggleUserAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userService.findById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            userService.toggleUserAccount(id, !user.isEnabled());
            
            String status = user.isEnabled() ? "disabled" : "enabled";
            redirectAttributes.addFlashAttribute("successMessage", 
                "User account has been " + status + " successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found!");
        }
        
        return "redirect:/admin/users";
    }

    /**
     * Unlock user account
     */
    @PostMapping("/users/{id}/unlock")
    public String unlockUserAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userService.findById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            userService.unlockUser(user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "User account unlocked successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found!");
        }
        
        return "redirect:/admin/users";
    }

    /**
     * Manage accounts page
     */
    @GetMapping("/accounts")
    public String manageAccounts(@RequestParam(value = "status", required = false) String status, Model model) {
        List<BankAccount> accounts;
        
        if (status != null && !status.isEmpty()) {
            try {
                BankAccount.AccountStatus accountStatus = BankAccount.AccountStatus.valueOf(status.toUpperCase());
                accounts = accountService.getAccountsByStatus(accountStatus);
                model.addAttribute("selectedStatus", status);
            } catch (IllegalArgumentException e) {
                accounts = accountService.getAllAccounts();
            }
        } else {
            accounts = accountService.getAllAccounts();
        }

        model.addAttribute("accounts", accounts);
        model.addAttribute("accountStatuses", BankAccount.AccountStatus.values());
        return "admin/manageAccounts";
    }

    /**
     * Account details page
     */
    @GetMapping("/accounts/{id}")
    public String accountDetails(@PathVariable Long id, Model model) {
        Optional<BankAccount> accountOptional = accountService.findById(id);
        if (accountOptional.isEmpty()) {
            return "redirect:/admin/accounts";
        }

        BankAccount account = accountOptional.get();
        List<Transaction> transactions = transactionService.getTransactionsByAccount(account);

        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions);
        model.addAttribute("transactionCount", transactions.size());

        return "admin/account-details";
    }

    /**
     * Approve account
     */
    @PostMapping("/accounts/{id}/approve")
    public String approveAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.approveAccount(id);
            redirectAttributes.addFlashAttribute("successMessage", "Account approved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve account: " + e.getMessage());
        }
        
        return "redirect:/admin/accounts";
    }

    /**
     * Reject account
     */
    @PostMapping("/accounts/{id}/reject")
    public String rejectAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.rejectAccount(id);
            redirectAttributes.addFlashAttribute("successMessage", "Account rejected successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject account: " + e.getMessage());
        }
        
        return "redirect:/admin/accounts";
    }

    /**
     * Suspend account
     */
    @PostMapping("/accounts/{id}/suspend")
    public String suspendAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.suspendAccount(id);
            redirectAttributes.addFlashAttribute("successMessage", "Account suspended successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to suspend account: " + e.getMessage());
        }
        
        return "redirect:/admin/accounts";
    }

    /**
     * Reactivate account
     */
    @PostMapping("/accounts/{id}/reactivate")
    public String reactivateAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.reactivateAccount(id);
            redirectAttributes.addFlashAttribute("successMessage", "Account reactivated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reactivate account: " + e.getMessage());
        }
        
        return "redirect:/admin/accounts";
    }

    /**
     * Monitor transactions page
     */
    @GetMapping("/transactions")
    public String monitorTransactions(@RequestParam(value = "status", required = false) String status, Model model) {
        List<Transaction> transactions;
        
        if (status != null && !status.isEmpty()) {
            try {
                Transaction.TransactionStatus transactionStatus = Transaction.TransactionStatus.valueOf(status.toUpperCase());
                transactions = transactionService.getTransactionsByStatus(transactionStatus);
                model.addAttribute("selectedStatus", status);
            } catch (IllegalArgumentException e) {
                transactions = transactionService.getAllTransactions();
            }
        } else {
            transactions = transactionService.getAllTransactions();
        }

        // Limit to recent 100 transactions for performance
        if (transactions.size() > 100) {
            transactions = transactions.subList(0, 100);
        }

        model.addAttribute("transactions", transactions);
        model.addAttribute("transactionStatuses", Transaction.TransactionStatus.values());
        return "admin/transactions";
    }

    /**
     * Transaction details page
     */
    @GetMapping("/transactions/{id}")
    public String transactionDetails(@PathVariable Long id, Model model) {
        Optional<Transaction> transactionOptional = transactionService.findById(id);
        if (transactionOptional.isEmpty()) {
            return "redirect:/admin/transactions";
        }

        Transaction transaction = transactionOptional.get();
        model.addAttribute("transaction", transaction);

        return "admin/transaction-details";
    }

    /**
     * Reports page
     */
    @GetMapping("/reports")
    public String reports(Model model) {
        UserService.UserStats userStats = userService.getUserStats();
        AccountService.AccountStats accountStats = accountService.getAccountStats();
        TransactionService.TransactionStats transactionStats = transactionService.getTransactionStats();

        model.addAttribute("userStats", userStats);
        model.addAttribute("accountStats", accountStats);
        model.addAttribute("transactionStats", transactionStats);

        return "admin/reports";
    }

    /**
     * System settings page
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        return "admin/settings";
    }
}