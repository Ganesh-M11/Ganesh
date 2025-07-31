package com.bankease.repository;

import com.bankease.model.BankAccount;
import com.bankease.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    
    /**
     * Find account by account number
     */
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    
    /**
     * Find all accounts for a user
     */
    List<BankAccount> findByUser(User user);
    
    /**
     * Find active accounts for a user
     */
    List<BankAccount> findByUserAndStatus(User user, BankAccount.AccountStatus status);
    
    /**
     * Find accounts by status
     */
    List<BankAccount> findByStatus(BankAccount.AccountStatus status);
    
    /**
     * Find accounts by type
     */
    List<BankAccount> findByAccountType(BankAccount.AccountType accountType);
    
    /**
     * Find accounts created between dates
     */
    @Query("SELECT ba FROM BankAccount ba WHERE ba.createdAt BETWEEN :startDate AND :endDate")
    List<BankAccount> findAccountsCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find accounts with balance greater than specified amount
     */
    @Query("SELECT ba FROM BankAccount ba WHERE ba.balance > :amount")
    List<BankAccount> findAccountsWithBalanceGreaterThan(@Param("amount") BigDecimal amount);
    
    /**
     * Find accounts with balance less than specified amount
     */
    @Query("SELECT ba FROM BankAccount ba WHERE ba.balance < :amount")
    List<BankAccount> findAccountsWithBalanceLessThan(@Param("amount") BigDecimal amount);
    
    /**
     * Get total balance for all active accounts
     */
    @Query("SELECT SUM(ba.balance) FROM BankAccount ba WHERE ba.status = 'ACTIVE'")
    BigDecimal getTotalActiveBalance();
    
    /**
     * Get total balance for user's active accounts
     */
    @Query("SELECT SUM(ba.balance) FROM BankAccount ba WHERE ba.user = :user AND ba.status = 'ACTIVE'")
    BigDecimal getTotalBalanceForUser(@Param("user") User user);
    
    /**
     * Count accounts by status
     */
    long countByStatus(BankAccount.AccountStatus status);
    
    /**
     * Count accounts by type
     */
    long countByAccountType(BankAccount.AccountType accountType);
    
    /**
     * Find user's accounts ordered by creation date
     */
    List<BankAccount> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Check if account number exists
     */
    boolean existsByAccountNumber(String accountNumber);
    
    /**
     * Find pending accounts for approval
     */
    @Query("SELECT ba FROM BankAccount ba WHERE ba.status = 'PENDING' ORDER BY ba.createdAt ASC")
    List<BankAccount> findPendingAccountsForApproval();
    
    /**
     * Find recently created accounts (last 30 days)
     */
    @Query("SELECT ba FROM BankAccount ba WHERE ba.createdAt >= :thirtyDaysAgo ORDER BY ba.createdAt DESC")
    List<BankAccount> findRecentAccounts(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
}