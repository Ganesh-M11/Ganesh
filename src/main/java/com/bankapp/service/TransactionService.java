package com.bankapp.service;

import com.bankapp.entity.Transaction;
import com.bankapp.entity.User;
import com.bankapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("5000.00");
    private static final BigDecimal DAILY_TRANSFER_LIMIT = new BigDecimal("10000.00");

    public Transaction deposit(Long userId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be greater than zero");
        }

        User user = userService.getUserById(userId);
        BigDecimal oldBalance = user.getAccountBalance();
        BigDecimal newBalance = oldBalance.add(amount);

        // Create transaction
        Transaction transaction = new Transaction(Transaction.TransactionType.DEPOSIT, amount, user);
        transaction.setDescription(description != null ? description : "Account deposit");
        transaction.setBalanceBefore(oldBalance);
        transaction.setBalanceAfter(newBalance);
        transaction.setReferenceNumber(generateReferenceNumber());
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setProcessedAt(LocalDateTime.now());

        // Update user balance
        userService.updateAccountBalance(userId, newBalance);

        // Save transaction
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Send notifications
        notificationService.createTransactionAlert(user, "deposit", amount, transaction.getDescription());

        if (amount.compareTo(LARGE_TRANSACTION_THRESHOLD) >= 0) {
            notificationService.createSecurityAlert(user, 
                "Large deposit of $" + amount + " has been processed on your account");
        }

        return savedTransaction;
    }

    public Transaction withdraw(Long userId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdrawal amount must be greater than zero");
        }

        User user = userService.getUserById(userId);
        BigDecimal currentBalance = user.getAccountBalance();

        if (currentBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        BigDecimal newBalance = currentBalance.subtract(amount);

        // Create transaction
        Transaction transaction = new Transaction(Transaction.TransactionType.WITHDRAWAL, amount, user);
        transaction.setDescription(description != null ? description : "Account withdrawal");
        transaction.setBalanceBefore(currentBalance);
        transaction.setBalanceAfter(newBalance);
        transaction.setReferenceNumber(generateReferenceNumber());
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setProcessedAt(LocalDateTime.now());

        // Update user balance
        userService.updateAccountBalance(userId, newBalance);

        // Save transaction
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Send notifications
        notificationService.createTransactionAlert(user, "withdrawal", amount, transaction.getDescription());

        if (amount.compareTo(LARGE_TRANSACTION_THRESHOLD) >= 0) {
            notificationService.createSecurityAlert(user, 
                "Large withdrawal of $" + amount + " has been processed on your account");
        }

        return savedTransaction;
    }

    public Transaction transfer(Long fromUserId, String toAccountNumber, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be greater than zero");
        }

        // Validate daily transfer limit
        if (amount.compareTo(DAILY_TRANSFER_LIMIT) > 0) {
            throw new RuntimeException("Transfer amount exceeds daily limit of $" + DAILY_TRANSFER_LIMIT);
        }

        User fromUser = userService.getUserById(fromUserId);
        User toUser = userService.getUserByAccountNumber(toAccountNumber);

        if (fromUser.getId().equals(toUser.getId())) {
            throw new RuntimeException("Cannot transfer to the same account");
        }

        BigDecimal fromBalance = fromUser.getAccountBalance();
        if (fromBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // Check daily transfer limit
        BigDecimal todayTransfers = getDailyTransferAmount(fromUserId);
        if (todayTransfers.add(amount).compareTo(DAILY_TRANSFER_LIMIT) > 0) {
            throw new RuntimeException("Daily transfer limit exceeded");
        }

        // Calculate new balances
        BigDecimal newFromBalance = fromBalance.subtract(amount);
        BigDecimal toBalance = toUser.getAccountBalance();
        BigDecimal newToBalance = toBalance.add(amount);

        String referenceNumber = generateReferenceNumber();

        // Create outgoing transaction
        Transaction outgoingTransaction = new Transaction(Transaction.TransactionType.TRANSFER_OUT, amount, fromUser);
        outgoingTransaction.setDescription(description != null ? description : "Transfer to " + toUser.getFullName());
        outgoingTransaction.setBalanceBefore(fromBalance);
        outgoingTransaction.setBalanceAfter(newFromBalance);
        outgoingTransaction.setReferenceNumber(referenceNumber);
        outgoingTransaction.setRecipientAccountNumber(toAccountNumber);
        outgoingTransaction.setRecipientName(toUser.getFullName());
        outgoingTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        outgoingTransaction.setProcessedAt(LocalDateTime.now());

        // Create incoming transaction
        Transaction incomingTransaction = new Transaction(Transaction.TransactionType.TRANSFER_IN, amount, toUser);
        incomingTransaction.setDescription(description != null ? description : "Transfer from " + fromUser.getFullName());
        incomingTransaction.setBalanceBefore(toBalance);
        incomingTransaction.setBalanceAfter(newToBalance);
        incomingTransaction.setReferenceNumber(referenceNumber);
        incomingTransaction.setRecipientAccountNumber(fromUser.getAccountNumber());
        incomingTransaction.setRecipientName(fromUser.getFullName());
        incomingTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        incomingTransaction.setProcessedAt(LocalDateTime.now());

        // Update balances
        userService.updateAccountBalance(fromUserId, newFromBalance);
        userService.updateAccountBalance(toUser.getId(), newToBalance);

        // Save transactions
        Transaction savedOutgoing = transactionRepository.save(outgoingTransaction);
        transactionRepository.save(incomingTransaction);

        // Send notifications
        notificationService.createTransactionAlert(fromUser, "transfer out", amount, 
            "Transfer to " + toUser.getFullName());
        notificationService.createTransactionAlert(toUser, "transfer in", amount, 
            "Transfer from " + fromUser.getFullName());

        if (amount.compareTo(LARGE_TRANSACTION_THRESHOLD) >= 0) {
            notificationService.createSecurityAlert(fromUser, 
                "Large transfer of $" + amount + " has been sent to " + toUser.getFullName());
            notificationService.createSecurityAlert(toUser, 
                "Large transfer of $" + amount + " has been received from " + fromUser.getFullName());
        }

        return savedOutgoing;
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public Transaction getTransactionByReferenceNumber(String referenceNumber) {
        return transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public List<Transaction> getUserTransactions(User user) {
        return transactionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Page<Transaction> getUserTransactions(User user, Pageable pageable) {
        return transactionRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public List<Transaction> getUserTransactionsByType(User user, Transaction.TransactionType type) {
        return transactionRepository.findByUserAndTransactionTypeOrderByCreatedAtDesc(user, type);
    }

    public Page<Transaction> getUserTransactionsByType(User user, Transaction.TransactionType type, Pageable pageable) {
        return transactionRepository.findByUserAndTransactionTypeOrderByCreatedAtDesc(user, type, pageable);
    }

    public List<Transaction> getUserTransactionsByDateRange(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByUserAndDateRange(user, startDate, endDate);
    }

    public Page<Transaction> getUserTransactionsByDateRange(User user, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return transactionRepository.findByUserAndDateRange(user, startDate, endDate, pageable);
    }

    public List<Transaction> getUserTransactionsByAmountRange(User user, BigDecimal minAmount, BigDecimal maxAmount) {
        return transactionRepository.findByUserAndAmountRange(user, minAmount, maxAmount);
    }

    public BigDecimal getTotalAmountByUserAndType(User user, Transaction.TransactionType type) {
        BigDecimal total = transactionRepository.sumAmountByUserAndType(user, type);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getDailyTransferAmount(Long userId) {
        User user = userService.getUserById(userId);
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        BigDecimal transferOut = transactionRepository.sumAmountByUserAndTypeAndDateRange(
            user, Transaction.TransactionType.TRANSFER_OUT, startOfDay, endOfDay);
        
        return transferOut != null ? transferOut : BigDecimal.ZERO;
    }

    public long getUserTransactionCount(User user) {
        return transactionRepository.countCompletedTransactionsByUser(user);
    }

    // Admin methods
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findPendingTransactions();
    }

    public List<Transaction> getLargeTransactions() {
        return transactionRepository.findLargeTransactions(LARGE_TRANSACTION_THRESHOLD);
    }

    public Page<Transaction> searchTransactions(String searchTerm, Pageable pageable) {
        return transactionRepository.searchTransactions(searchTerm, pageable);
    }

    public List<Object[]> getMonthlyTransactionSummary() {
        return transactionRepository.getMonthlyTransactionSummary();
    }

    public List<Object[]> getDailyTransactionSummaryForUser(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.getDailyTransactionSummaryForUser(user, startDate, endDate);
    }

    private String generateReferenceNumber() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}