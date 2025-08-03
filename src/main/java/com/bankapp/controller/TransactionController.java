package com.bankapp.controller;

import com.bankapp.entity.Transaction;
import com.bankapp.entity.User;
import com.bankapp.security.CustomUserDetailsService;
import com.bankapp.service.TransactionService;
import com.bankapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    // Web endpoints
    @GetMapping
    public String transactionHistory(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Transaction> transactions;
        
        if (type != null && !type.isEmpty()) {
            Transaction.TransactionType transactionType = Transaction.TransactionType.valueOf(type.toUpperCase());
            transactions = transactionService.getUserTransactionsByType(currentUser, transactionType, pageable);
        } else if (startDate != null && endDate != null) {
            transactions = transactionService.getUserTransactionsByDateRange(currentUser, startDate, endDate, pageable);
        } else {
            transactions = transactionService.getUserTransactions(currentUser, pageable);
        }
        
        model.addAttribute("user", currentUser);
        model.addAttribute("transactions", transactions);
        model.addAttribute("selectedType", type);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("transactionTypes", Transaction.TransactionType.values());
        
        return "transactions/history";
    }

    @GetMapping("/deposit")
    public String depositPage(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("user", currentUser);
        model.addAttribute("depositRequest", new TransactionRequest());
        return "transactions/deposit";
    }

    @PostMapping("/deposit")
    public String processDeposit(@Valid @ModelAttribute("depositRequest") TransactionRequest request,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            User currentUser = getCurrentUser();
            model.addAttribute("user", currentUser);
            return "transactions/deposit";
        }

        try {
            User currentUser = getCurrentUser();
            Transaction transaction = transactionService.deposit(currentUser.getId(), request.getAmount(), request.getDescription());
            
            redirectAttributes.addFlashAttribute("success", 
                "Deposit of $" + request.getAmount() + " processed successfully. Reference: " + transaction.getReferenceNumber());
            return "redirect:/dashboard";
        } catch (RuntimeException e) {
            User currentUser = getCurrentUser();
            model.addAttribute("user", currentUser);
            model.addAttribute("error", e.getMessage());
            return "transactions/deposit";
        }
    }

    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("user", currentUser);
        model.addAttribute("withdrawRequest", new TransactionRequest());
        model.addAttribute("currentBalance", currentUser.getAccountBalance());
        return "transactions/withdraw";
    }

    @PostMapping("/withdraw")
    public String processWithdraw(@Valid @ModelAttribute("withdrawRequest") TransactionRequest request,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            User currentUser = getCurrentUser();
            model.addAttribute("user", currentUser);
            model.addAttribute("currentBalance", currentUser.getAccountBalance());
            return "transactions/withdraw";
        }

        try {
            User currentUser = getCurrentUser();
            Transaction transaction = transactionService.withdraw(currentUser.getId(), request.getAmount(), request.getDescription());
            
            redirectAttributes.addFlashAttribute("success", 
                "Withdrawal of $" + request.getAmount() + " processed successfully. Reference: " + transaction.getReferenceNumber());
            return "redirect:/dashboard";
        } catch (RuntimeException e) {
            User currentUser = getCurrentUser();
            model.addAttribute("user", currentUser);
            model.addAttribute("currentBalance", currentUser.getAccountBalance());
            model.addAttribute("error", e.getMessage());
            return "transactions/withdraw";
        }
    }

    @GetMapping("/transfer")
    public String transferPage(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("user", currentUser);
        model.addAttribute("transferRequest", new TransferRequest());
        model.addAttribute("currentBalance", currentUser.getAccountBalance());
        
        // Calculate remaining daily transfer limit
        BigDecimal dailyTransferred = transactionService.getDailyTransferAmount(currentUser.getId());
        BigDecimal dailyLimit = new BigDecimal("10000.00");
        model.addAttribute("remainingDailyLimit", dailyLimit.subtract(dailyTransferred));
        
        return "transactions/transfer";
    }

    @PostMapping("/transfer")
    public String processTransfer(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            User currentUser = getCurrentUser();
            model.addAttribute("user", currentUser);
            model.addAttribute("currentBalance", currentUser.getAccountBalance());
            
            BigDecimal dailyTransferred = transactionService.getDailyTransferAmount(currentUser.getId());
            BigDecimal dailyLimit = new BigDecimal("10000.00");
            model.addAttribute("remainingDailyLimit", dailyLimit.subtract(dailyTransferred));
            
            return "transactions/transfer";
        }

        try {
            User currentUser = getCurrentUser();
            Transaction transaction = transactionService.transfer(
                currentUser.getId(), 
                request.getRecipientAccountNumber(), 
                request.getAmount(), 
                request.getDescription()
            );
            
            redirectAttributes.addFlashAttribute("success", 
                "Transfer of $" + request.getAmount() + " processed successfully. Reference: " + transaction.getReferenceNumber());
            return "redirect:/dashboard";
        } catch (RuntimeException e) {
            User currentUser = getCurrentUser();
            model.addAttribute("user", currentUser);
            model.addAttribute("currentBalance", currentUser.getAccountBalance());
            
            BigDecimal dailyTransferred = transactionService.getDailyTransferAmount(currentUser.getId());
            BigDecimal dailyLimit = new BigDecimal("10000.00");
            model.addAttribute("remainingDailyLimit", dailyLimit.subtract(dailyTransferred));
            
            model.addAttribute("error", e.getMessage());
            return "transactions/transfer";
        }
    }

    @GetMapping("/{id}")
    public String transactionDetails(@PathVariable Long id, Model model) {
        try {
            Transaction transaction = transactionService.getTransactionById(id);
            User currentUser = getCurrentUser();
            
            // Ensure user can only view their own transactions
            if (!transaction.getUser().getId().equals(currentUser.getId())) {
                return "redirect:/transactions?error=unauthorized";
            }
            
            model.addAttribute("transaction", transaction);
            model.addAttribute("user", currentUser);
            return "transactions/details";
        } catch (RuntimeException e) {
            return "redirect:/transactions?error=notfound";
        }
    }

    // API endpoints
    @PostMapping("/api/deposit")
    @ResponseBody
    public ResponseEntity<?> apiDeposit(@Valid @RequestBody TransactionRequest request) {
        try {
            User currentUser = getCurrentUser();
            Transaction transaction = transactionService.deposit(currentUser.getId(), request.getAmount(), request.getDescription());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Deposit processed successfully");
            response.put("transaction", createTransactionResponse(transaction));
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/withdraw")
    @ResponseBody
    public ResponseEntity<?> apiWithdraw(@Valid @RequestBody TransactionRequest request) {
        try {
            User currentUser = getCurrentUser();
            Transaction transaction = transactionService.withdraw(currentUser.getId(), request.getAmount(), request.getDescription());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Withdrawal processed successfully");
            response.put("transaction", createTransactionResponse(transaction));
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/transfer")
    @ResponseBody
    public ResponseEntity<?> apiTransfer(@Valid @RequestBody TransferRequest request) {
        try {
            User currentUser = getCurrentUser();
            Transaction transaction = transactionService.transfer(
                currentUser.getId(), 
                request.getRecipientAccountNumber(), 
                request.getAmount(), 
                request.getDescription()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer processed successfully");
            response.put("transaction", createTransactionResponse(transaction));
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/history")
    @ResponseBody
    public ResponseEntity<?> apiTransactionHistory(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        try {
            User currentUser = getCurrentUser();
            Pageable pageable = PageRequest.of(page, size);
            Page<Transaction> transactions = transactionService.getUserTransactions(currentUser, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("transactions", transactions.getContent().stream()
                .map(this::createTransactionResponse).toList());
            response.put("totalElements", transactions.getTotalElements());
            response.put("totalPages", transactions.getTotalPages());
            response.put("currentPage", page);
            
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

    private Map<String, Object> createTransactionResponse(Transaction transaction) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", transaction.getId());
        response.put("type", transaction.getTransactionType().name());
        response.put("amount", transaction.getAmount());
        response.put("description", transaction.getDescription());
        response.put("referenceNumber", transaction.getReferenceNumber());
        response.put("status", transaction.getStatus().name());
        response.put("balanceBefore", transaction.getBalanceBefore());
        response.put("balanceAfter", transaction.getBalanceAfter());
        response.put("createdAt", transaction.getCreatedAt());
        response.put("processedAt", transaction.getProcessedAt());
        
        if (transaction.getRecipientAccountNumber() != null) {
            response.put("recipientAccountNumber", transaction.getRecipientAccountNumber());
            response.put("recipientName", transaction.getRecipientName());
        }
        
        return response;
    }

    // DTO classes
    public static class TransactionRequest {
        private BigDecimal amount;
        private String description;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class TransferRequest extends TransactionRequest {
        private String recipientAccountNumber;

        public String getRecipientAccountNumber() { return recipientAccountNumber; }
        public void setRecipientAccountNumber(String recipientAccountNumber) { this.recipientAccountNumber = recipientAccountNumber; }
    }
}