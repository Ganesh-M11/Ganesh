package com.bankease.controller;

import com.bankease.model.BankAccount;
import com.bankease.model.Transaction;
import com.bankease.model.User;
import com.bankease.service.AccountService;
import com.bankease.service.TransactionService;
import com.bankease.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userService.findByUsername(auth.getName()).orElse(null);
    }

    /**
     * User dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<BankAccount> accounts = accountService.getAccountsByUser(currentUser);
        List<BankAccount> activeAccounts = accountService.getActiveAccountsByUser(currentUser);
        BigDecimal totalBalance = accountService.getTotalBalanceForUser(currentUser);
        List<Transaction> recentTransactions = transactionService.getTransactionsByUserId(currentUser.getId());
        
        // Limit recent transactions to 5
        if (recentTransactions.size() > 5) {
            recentTransactions = recentTransactions.subList(0, 5);
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("accounts", accounts);
        model.addAttribute("activeAccounts", activeAccounts);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("accountCount", accounts.size());
        model.addAttribute("activeAccountCount", activeAccounts.size());

        return "dashboard";
    }

    /**
     * Open new account page
     */
    @GetMapping("/open-account")
    public String openAccountPage(Model model) {
        model.addAttribute("accountTypes", BankAccount.AccountType.values());
        return "user/open-account";
    }

    /**
     * Process new account opening
     */
    @PostMapping("/open-account")
    public String openAccount(@RequestParam("accountType") BankAccount.AccountType accountType,
                             RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            BankAccount newAccount = accountService.createAccount(accountType, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Account application submitted successfully! Account Number: " + newAccount.getAccountNumber() + 
                ". Your account is pending approval.");
            return "redirect:/user/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create account: " + e.getMessage());
            return "redirect:/user/open-account";
        }
    }

    /**
     * Account details page
     */
    @GetMapping("/account/{id}")
    public String accountDetails(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<BankAccount> accountOptional = accountService.findById(id);
        if (accountOptional.isEmpty() || !accountOptional.get().getUser().getId().equals(currentUser.getId())) {
            return "redirect:/user/dashboard";
        }

        BankAccount account = accountOptional.get();
        List<Transaction> transactions = transactionService.getTransactionsByAccount(account);

        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions);
        model.addAttribute("transactionCount", transactions.size());

        return "user/account-details";
    }

    /**
     * Deposit page
     */
    @GetMapping("/deposit")
    public String depositPage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<BankAccount> activeAccounts = accountService.getActiveAccountsByUser(currentUser);
        model.addAttribute("accounts", activeAccounts);
        return "user/deposit";
    }

    /**
     * Process deposit
     */
    @PostMapping("/deposit")
    public String processDeposit(@RequestParam("accountId") Long accountId,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam(value = "description", required = false) String description,
                                RedirectAttributes redirectAttributes) {
        try {
            Transaction transaction = transactionService.deposit(accountId, amount, description);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Deposit successful! Transaction ID: " + transaction.getTransactionId());
            return "redirect:/user/account/" + accountId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Deposit failed: " + e.getMessage());
            return "redirect:/user/deposit";
        }
    }

    /**
     * Withdraw page
     */
    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<BankAccount> activeAccounts = accountService.getActiveAccountsByUser(currentUser);
        model.addAttribute("accounts", activeAccounts);
        return "user/withdraw";
    }

    /**
     * Process withdrawal
     */
    @PostMapping("/withdraw")
    public String processWithdraw(@RequestParam("accountId") Long accountId,
                                 @RequestParam("amount") BigDecimal amount,
                                 @RequestParam(value = "description", required = false) String description,
                                 RedirectAttributes redirectAttributes) {
        try {
            Transaction transaction = transactionService.withdraw(accountId, amount, description);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Withdrawal successful! Transaction ID: " + transaction.getTransactionId());
            return "redirect:/user/account/" + accountId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Withdrawal failed: " + e.getMessage());
            return "redirect:/user/withdraw";
        }
    }

    /**
     * Transaction history page
     */
    @GetMapping("/transactions")
    public String transactionHistory(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Page<Transaction> transactionPage = transactionService.getTransactionsByUserId(currentUser.getId(), page, size);
        
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionPage.getTotalPages());
        model.addAttribute("totalElements", transactionPage.getTotalElements());
        model.addAttribute("hasNext", transactionPage.hasNext());
        model.addAttribute("hasPrevious", transactionPage.hasPrevious());

        return "user/transactions";
    }

    /**
     * Profile page
     */
    @GetMapping("/profile")
    public String profile(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", currentUser);
        return "user/profile";
    }

    /**
     * Update profile
     */
    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("user") User user,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            return "user/profile";
        }

        try {
            // Update only allowed fields
            currentUser.setFirstName(user.getFirstName());
            currentUser.setLastName(user.getLastName());
            currentUser.setEmail(user.getEmail());
            currentUser.setPhoneNumber(user.getPhoneNumber());
            currentUser.setAddress(user.getAddress());

            userService.updateUser(currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            return "redirect:/user/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Profile update failed: " + e.getMessage());
            return "user/profile";
        }
    }

    /**
     * Change password page
     */
    @GetMapping("/change-password")
    public String changePasswordPage() {
        return "user/change-password";
    }

    /**
     * Process password change
     */
    @PostMapping("/change-password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match!");
            return "redirect:/user/change-password";
        }

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 6 characters long!");
            return "redirect:/user/change-password";
        }

        try {
            userService.updatePassword(currentUser, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully!");
            return "redirect:/user/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password change failed: " + e.getMessage());
            return "redirect:/user/change-password";
        }
    }

    /**
     * Get account balance (AJAX endpoint)
     */
    @GetMapping("/account/{id}/balance")
    @ResponseBody
    public BigDecimal getAccountBalance(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return BigDecimal.ZERO;
        }

        Optional<BankAccount> accountOptional = accountService.findById(id);
        if (accountOptional.isPresent() && accountOptional.get().getUser().getId().equals(currentUser.getId())) {
            return accountOptional.get().getBalance();
        }
        return BigDecimal.ZERO;
    }
}