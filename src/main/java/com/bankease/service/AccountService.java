package com.bankease.service;

import com.bankease.model.BankAccount;
import com.bankease.model.User;
import com.bankease.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AccountService {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    /**
     * Create a new bank account
     */
    public BankAccount createAccount(BankAccount.AccountType accountType, User user) {
        BankAccount account = new BankAccount(accountType, user);
        return bankAccountRepository.save(account);
    }

    /**
     * Find account by ID
     */
    public Optional<BankAccount> findById(Long id) {
        return bankAccountRepository.findById(id);
    }

    /**
     * Find account by account number
     */
    public Optional<BankAccount> findByAccountNumber(String accountNumber) {
        return bankAccountRepository.findByAccountNumber(accountNumber);
    }

    /**
     * Get all accounts for a user
     */
    public List<BankAccount> getAccountsByUser(User user) {
        return bankAccountRepository.findByUser(user);
    }

    /**
     * Get active accounts for a user
     */
    public List<BankAccount> getActiveAccountsByUser(User user) {
        return bankAccountRepository.findByUserAndStatus(user, BankAccount.AccountStatus.ACTIVE);
    }

    /**
     * Get all accounts
     */
    public List<BankAccount> getAllAccounts() {
        return bankAccountRepository.findAll();
    }

    /**
     * Get accounts by status
     */
    public List<BankAccount> getAccountsByStatus(BankAccount.AccountStatus status) {
        return bankAccountRepository.findByStatus(status);
    }

    /**
     * Get pending accounts for approval
     */
    public List<BankAccount> getPendingAccounts() {
        return bankAccountRepository.findPendingAccountsForApproval();
    }

    /**
     * Approve account
     */
    public void approveAccount(Long accountId) {
        Optional<BankAccount> accountOptional = bankAccountRepository.findById(accountId);
        if (accountOptional.isPresent()) {
            BankAccount account = accountOptional.get();
            account.setStatus(BankAccount.AccountStatus.ACTIVE);
            bankAccountRepository.save(account);
        } else {
            throw new RuntimeException("Account not found");
        }
    }

    /**
     * Reject account
     */
    public void rejectAccount(Long accountId) {
        Optional<BankAccount> accountOptional = bankAccountRepository.findById(accountId);
        if (accountOptional.isPresent()) {
            BankAccount account = accountOptional.get();
            account.setStatus(BankAccount.AccountStatus.CLOSED);
            bankAccountRepository.save(account);
        } else {
            throw new RuntimeException("Account not found");
        }
    }

    /**
     * Suspend account
     */
    public void suspendAccount(Long accountId) {
        Optional<BankAccount> accountOptional = bankAccountRepository.findById(accountId);
        if (accountOptional.isPresent()) {
            BankAccount account = accountOptional.get();
            account.setStatus(BankAccount.AccountStatus.SUSPENDED);
            bankAccountRepository.save(account);
        } else {
            throw new RuntimeException("Account not found");
        }
    }

    /**
     * Reactivate suspended account
     */
    public void reactivateAccount(Long accountId) {
        Optional<BankAccount> accountOptional = bankAccountRepository.findById(accountId);
        if (accountOptional.isPresent()) {
            BankAccount account = accountOptional.get();
            if (account.getStatus() == BankAccount.AccountStatus.SUSPENDED) {
                account.setStatus(BankAccount.AccountStatus.ACTIVE);
                bankAccountRepository.save(account);
            } else {
                throw new RuntimeException("Account is not suspended");
            }
        } else {
            throw new RuntimeException("Account not found");
        }
    }

    /**
     * Close account
     */
    public void closeAccount(Long accountId) {
        Optional<BankAccount> accountOptional = bankAccountRepository.findById(accountId);
        if (accountOptional.isPresent()) {
            BankAccount account = accountOptional.get();
            if (account.getBalance().compareTo(BigDecimal.ZERO) == 0) {
                account.setStatus(BankAccount.AccountStatus.CLOSED);
                bankAccountRepository.save(account);
            } else {
                throw new RuntimeException("Cannot close account with non-zero balance");
            }
        } else {
            throw new RuntimeException("Account not found");
        }
    }

    /**
     * Update account balance
     */
    public BankAccount updateBalance(Long accountId, BigDecimal newBalance) {
        Optional<BankAccount> accountOptional = bankAccountRepository.findById(accountId);
        if (accountOptional.isPresent()) {
            BankAccount account = accountOptional.get();
            account.setBalance(newBalance);
            return bankAccountRepository.save(account);
        } else {
            throw new RuntimeException("Account not found");
        }
    }

    /**
     * Get total balance for user
     */
    public BigDecimal getTotalBalanceForUser(User user) {
        BigDecimal total = bankAccountRepository.getTotalBalanceForUser(user);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Get total active balance in the system
     */
    public BigDecimal getTotalActiveBalance() {
        BigDecimal total = bankAccountRepository.getTotalActiveBalance();
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Check if account is active and can perform transactions
     */
    public boolean isAccountActiveForTransactions(BankAccount account) {
        return account.getStatus() == BankAccount.AccountStatus.ACTIVE;
    }

    /**
     * Validate account for transaction
     */
    public void validateAccountForTransaction(BankAccount account) {
        if (account == null) {
            throw new RuntimeException("Account not found");
        }
        
        if (account.getStatus() != BankAccount.AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is not active for transactions");
        }
    }

    /**
     * Get account statistics
     */
    public AccountStats getAccountStats() {
        long totalAccounts = bankAccountRepository.count();
        long activeAccounts = bankAccountRepository.countByStatus(BankAccount.AccountStatus.ACTIVE);
        long pendingAccounts = bankAccountRepository.countByStatus(BankAccount.AccountStatus.PENDING);
        long suspendedAccounts = bankAccountRepository.countByStatus(BankAccount.AccountStatus.SUSPENDED);
        long closedAccounts = bankAccountRepository.countByStatus(BankAccount.AccountStatus.CLOSED);
        
        long savingsAccounts = bankAccountRepository.countByAccountType(BankAccount.AccountType.SAVINGS);
        long checkingAccounts = bankAccountRepository.countByAccountType(BankAccount.AccountType.CHECKING);
        long businessAccounts = bankAccountRepository.countByAccountType(BankAccount.AccountType.BUSINESS);
        
        BigDecimal totalBalance = getTotalActiveBalance();
        
        return new AccountStats(totalAccounts, activeAccounts, pendingAccounts, suspendedAccounts, 
                               closedAccounts, savingsAccounts, checkingAccounts, businessAccounts, totalBalance);
    }

    /**
     * Get recently created accounts
     */
    public List<BankAccount> getRecentAccounts() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return bankAccountRepository.findRecentAccounts(thirtyDaysAgo);
    }

    /**
     * Search accounts by account number pattern
     */
    public List<BankAccount> searchAccountsByNumber(String pattern) {
        return bankAccountRepository.findAll().stream()
                .filter(account -> account.getAccountNumber().contains(pattern))
                .toList();
    }

    /**
     * Get accounts with low balance
     */
    public List<BankAccount> getAccountsWithLowBalance(BigDecimal threshold) {
        return bankAccountRepository.findAccountsWithBalanceLessThan(threshold);
    }

    /**
     * Get accounts with high balance
     */
    public List<BankAccount> getAccountsWithHighBalance(BigDecimal threshold) {
        return bankAccountRepository.findAccountsWithBalanceGreaterThan(threshold);
    }

    // Inner class for account statistics
    public static class AccountStats {
        private long totalAccounts;
        private long activeAccounts;
        private long pendingAccounts;
        private long suspendedAccounts;
        private long closedAccounts;
        private long savingsAccounts;
        private long checkingAccounts;
        private long businessAccounts;
        private BigDecimal totalBalance;

        public AccountStats(long totalAccounts, long activeAccounts, long pendingAccounts, 
                           long suspendedAccounts, long closedAccounts, long savingsAccounts, 
                           long checkingAccounts, long businessAccounts, BigDecimal totalBalance) {
            this.totalAccounts = totalAccounts;
            this.activeAccounts = activeAccounts;
            this.pendingAccounts = pendingAccounts;
            this.suspendedAccounts = suspendedAccounts;
            this.closedAccounts = closedAccounts;
            this.savingsAccounts = savingsAccounts;
            this.checkingAccounts = checkingAccounts;
            this.businessAccounts = businessAccounts;
            this.totalBalance = totalBalance;
        }

        // Getters
        public long getTotalAccounts() { return totalAccounts; }
        public long getActiveAccounts() { return activeAccounts; }
        public long getPendingAccounts() { return pendingAccounts; }
        public long getSuspendedAccounts() { return suspendedAccounts; }
        public long getClosedAccounts() { return closedAccounts; }
        public long getSavingsAccounts() { return savingsAccounts; }
        public long getCheckingAccounts() { return checkingAccounts; }
        public long getBusinessAccounts() { return businessAccounts; }
        public BigDecimal getTotalBalance() { return totalBalance; }
    }
}