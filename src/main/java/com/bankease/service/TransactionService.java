package com.bankease.service;

import com.bankease.model.BankAccount;
import com.bankease.model.Transaction;
import com.bankease.repository.BankAccountRepository;
import com.bankease.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private AccountService accountService;

    /**
     * Process deposit transaction
     */
    public Transaction deposit(Long accountId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be greater than zero");
        }

        Optional<BankAccount> accountOptional = bankAccountRepository.findById(accountId);
        if (accountOptional.isEmpty()) {
            throw new RuntimeException("Account not found");
        }

        BankAccount account = accountOptional.get();
        accountService.validateAccountForTransaction(account);

        // Create transaction record
        Transaction transaction = new Transaction(
            Transaction.TransactionType.DEPOSIT,
            amount,
            description != null ? description : "Deposit",
            account
        );

        transaction.setBalanceBefore(account.getBalance());

        // Update account balance
        account.deposit(amount);
        transaction.setBalanceAfter(account.getBalance());

        // Save transaction and updated account
        bankAccountRepository.save(account);
        return transactionRepository.save(transaction);
    }

    /**
     * Process withdrawal transaction
     */
    public Transaction withdraw(Long accountId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdrawal amount must be greater than zero");
        }

        Optional<BankAccount> accountOptional = bankAccountRepository.findById(accountId);
        if (accountOptional.isEmpty()) {
            throw new RuntimeException("Account not found");
        }

        BankAccount account = accountOptional.get();
        accountService.validateAccountForTransaction(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // Create transaction record
        Transaction transaction = new Transaction(
            Transaction.TransactionType.WITHDRAWAL,
            amount,
            description != null ? description : "Withdrawal",
            account
        );

        transaction.setBalanceBefore(account.getBalance());

        // Update account balance
        boolean success = account.withdraw(amount);
        if (!success) {
            throw new RuntimeException("Withdrawal failed");
        }

        transaction.setBalanceAfter(account.getBalance());

        // Save transaction and updated account
        bankAccountRepository.save(account);
        return transactionRepository.save(transaction);
    }

    /**
     * Transfer money between accounts
     */
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be greater than zero");
        }

        if (fromAccountId.equals(toAccountId)) {
            throw new RuntimeException("Cannot transfer to the same account");
        }

        // Withdraw from source account
        Transaction withdrawalTransaction = withdraw(fromAccountId, amount, 
            "Transfer to account " + toAccountId + (description != null ? " - " + description : ""));
        withdrawalTransaction.setTransactionType(Transaction.TransactionType.TRANSFER_OUT);
        transactionRepository.save(withdrawalTransaction);

        // Deposit to destination account
        Transaction depositTransaction = deposit(toAccountId, amount, 
            "Transfer from account " + fromAccountId + (description != null ? " - " + description : ""));
        depositTransaction.setTransactionType(Transaction.TransactionType.TRANSFER_IN);
        transactionRepository.save(depositTransaction);
    }

    /**
     * Get transaction by ID
     */
    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    /**
     * Get transaction by transaction ID
     */
    public Optional<Transaction> findByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }

    /**
     * Get all transactions for an account
     */
    public List<Transaction> getTransactionsByAccount(BankAccount account) {
        return transactionRepository.findByAccountOrderByCreatedAtDesc(account);
    }

    /**
     * Get transactions for an account with pagination
     */
    public Page<Transaction> getTransactionsByAccount(BankAccount account, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByAccount(account, pageable);
    }

    /**
     * Get recent transactions for an account
     */
    public List<Transaction> getRecentTransactions(BankAccount account, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return transactionRepository.findRecentTransactions(account, pageable);
    }

    /**
     * Get transactions for user's accounts
     */
    public List<Transaction> getTransactionsByUserId(Long userId) {
        return transactionRepository.findTransactionsByUserId(userId);
    }

    /**
     * Get transactions for user's accounts with pagination
     */
    public Page<Transaction> getTransactionsByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findTransactionsByUserId(userId, pageable);
    }

    /**
     * Get transactions between dates
     */
    public List<Transaction> getTransactionsBetweenDates(BankAccount account, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findTransactionsBetweenDates(account, startDate, endDate);
    }

    /**
     * Get deposits for an account
     */
    public List<Transaction> getDepositsByAccount(BankAccount account) {
        return transactionRepository.findDepositsByAccount(account);
    }

    /**
     * Get withdrawals for an account
     */
    public List<Transaction> getWithdrawalsByAccount(BankAccount account) {
        return transactionRepository.findWithdrawalsByAccount(account);
    }

    /**
     * Get today's transactions
     */
    public List<Transaction> getTodaysTransactions() {
        return transactionRepository.findTodaysTransactions();
    }

    /**
     * Get all transactions
     */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Get transactions by status
     */
    public List<Transaction> getTransactionsByStatus(Transaction.TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    /**
     * Get pending transactions
     */
    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findPendingTransactions();
    }

    /**
     * Get transaction statistics
     */
    public TransactionStats getTransactionStats() {
        long totalTransactions = transactionRepository.count();
        long completedTransactions = transactionRepository.countByStatus(Transaction.TransactionStatus.COMPLETED);
        long pendingTransactions = transactionRepository.countByStatus(Transaction.TransactionStatus.PENDING);
        long failedTransactions = transactionRepository.countByStatus(Transaction.TransactionStatus.FAILED);
        
        long deposits = transactionRepository.countByTransactionType(Transaction.TransactionType.DEPOSIT);
        long withdrawals = transactionRepository.countByTransactionType(Transaction.TransactionType.WITHDRAWAL);
        long transfers = transactionRepository.countByTransactionType(Transaction.TransactionType.TRANSFER_IN) +
                        transactionRepository.countByTransactionType(Transaction.TransactionType.TRANSFER_OUT);
        
        return new TransactionStats(totalTransactions, completedTransactions, pendingTransactions, 
                                   failedTransactions, deposits, withdrawals, transfers);
    }

    /**
     * Get monthly transaction summary
     */
    public List<Object[]> getMonthlyTransactionSummary(LocalDateTime startOfMonth, LocalDateTime startOfNextMonth) {
        return transactionRepository.getMonthlyTransactionSummary(startOfMonth, startOfNextMonth);
    }

    /**
     * Calculate total amount by transaction type for account
     */
    public BigDecimal getTotalAmountByTypeAndAccount(BankAccount account, Transaction.TransactionType type) {
        BigDecimal total = transactionRepository.getTotalAmountByTypeAndAccount(account, type);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Get transaction count for account
     */
    public long getTransactionCountByAccount(BankAccount account) {
        return transactionRepository.countByAccount(account);
    }

    /**
     * Search transactions by amount
     */
    public List<Transaction> searchTransactionsByAmount(BigDecimal amount) {
        return transactionRepository.findTransactionsWithAmountGreaterThan(amount);
    }

    /**
     * Validate transaction amount
     */
    private void validateTransactionAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transaction amount must be greater than zero");
        }
        
        // Maximum transaction limit (e.g., $10,000)
        BigDecimal maxLimit = new BigDecimal("10000.00");
        if (amount.compareTo(maxLimit) > 0) {
            throw new RuntimeException("Transaction amount exceeds maximum limit of $" + maxLimit);
        }
    }

    /**
     * Cancel pending transaction
     */
    public void cancelTransaction(Long transactionId) {
        Optional<Transaction> transactionOptional = transactionRepository.findById(transactionId);
        if (transactionOptional.isPresent()) {
            Transaction transaction = transactionOptional.get();
            if (transaction.getStatus() == Transaction.TransactionStatus.PENDING) {
                transaction.setStatus(Transaction.TransactionStatus.CANCELLED);
                transactionRepository.save(transaction);
            } else {
                throw new RuntimeException("Only pending transactions can be cancelled");
            }
        } else {
            throw new RuntimeException("Transaction not found");
        }
    }

    // Inner class for transaction statistics
    public static class TransactionStats {
        private long totalTransactions;
        private long completedTransactions;
        private long pendingTransactions;
        private long failedTransactions;
        private long deposits;
        private long withdrawals;
        private long transfers;

        public TransactionStats(long totalTransactions, long completedTransactions, long pendingTransactions,
                               long failedTransactions, long deposits, long withdrawals, long transfers) {
            this.totalTransactions = totalTransactions;
            this.completedTransactions = completedTransactions;
            this.pendingTransactions = pendingTransactions;
            this.failedTransactions = failedTransactions;
            this.deposits = deposits;
            this.withdrawals = withdrawals;
            this.transfers = transfers;
        }

        // Getters
        public long getTotalTransactions() { return totalTransactions; }
        public long getCompletedTransactions() { return completedTransactions; }
        public long getPendingTransactions() { return pendingTransactions; }
        public long getFailedTransactions() { return failedTransactions; }
        public long getDeposits() { return deposits; }
        public long getWithdrawals() { return withdrawals; }
        public long getTransfers() { return transfers; }
    }
}