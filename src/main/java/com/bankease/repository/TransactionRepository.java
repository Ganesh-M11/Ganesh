package com.bankease.repository;

import com.bankease.model.BankAccount;
import com.bankease.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Find transaction by transaction ID
     */
    Optional<Transaction> findByTransactionId(String transactionId);
    
    /**
     * Find all transactions for an account
     */
    List<Transaction> findByAccount(BankAccount account);
    
    /**
     * Find transactions for an account with pagination
     */
    Page<Transaction> findByAccount(BankAccount account, Pageable pageable);
    
    /**
     * Find transactions for an account ordered by date (most recent first)
     */
    List<Transaction> findByAccountOrderByCreatedAtDesc(BankAccount account);
    
    /**
     * Find transactions by type
     */
    List<Transaction> findByTransactionType(Transaction.TransactionType transactionType);
    
    /**
     * Find transactions by status
     */
    List<Transaction> findByStatus(Transaction.TransactionStatus status);
    
    /**
     * Find transactions for an account between dates
     */
    @Query("SELECT t FROM Transaction t WHERE t.account = :account AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsBetweenDates(@Param("account") BankAccount account, 
                                                  @Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find transactions with amount greater than specified value
     */
    @Query("SELECT t FROM Transaction t WHERE t.amount > :amount ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsWithAmountGreaterThan(@Param("amount") BigDecimal amount);
    
    /**
     * Find recent transactions for an account (last N transactions)
     */
    @Query("SELECT t FROM Transaction t WHERE t.account = :account ORDER BY t.createdAt DESC")
    List<Transaction> findRecentTransactions(@Param("account") BankAccount account, Pageable pageable);
    
    /**
     * Get total amount for transaction type and account
     */
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account = :account AND t.transactionType = :type AND t.status = 'COMPLETED'")
    BigDecimal getTotalAmountByTypeAndAccount(@Param("account") BankAccount account, @Param("type") Transaction.TransactionType type);
    
    /**
     * Get transaction count for an account
     */
    long countByAccount(BankAccount account);
    
    /**
     * Get transaction count by type
     */
    long countByTransactionType(Transaction.TransactionType transactionType);
    
    /**
     * Get transaction count by status
     */
    long countByStatus(Transaction.TransactionStatus status);
    
    /**
     * Find deposits for an account
     */
    @Query("SELECT t FROM Transaction t WHERE t.account = :account AND t.transactionType = 'DEPOSIT' ORDER BY t.createdAt DESC")
    List<Transaction> findDepositsByAccount(@Param("account") BankAccount account);
    
    /**
     * Find withdrawals for an account
     */
    @Query("SELECT t FROM Transaction t WHERE t.account = :account AND t.transactionType = 'WITHDRAWAL' ORDER BY t.createdAt DESC")
    List<Transaction> findWithdrawalsByAccount(@Param("account") BankAccount account);
    
    /**
     * Find transactions created today
     */
    @Query("SELECT t FROM Transaction t WHERE DATE(t.createdAt) = CURRENT_DATE ORDER BY t.createdAt DESC")
    List<Transaction> findTodaysTransactions();
    
    /**
     * Find transactions for user's accounts
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsByUserId(@Param("userId") Long userId);
    
    /**
     * Find transactions for user's accounts with pagination
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findTransactionsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Get monthly transaction summary
     */
    @Query("SELECT t.transactionType, COUNT(t), SUM(t.amount) FROM Transaction t WHERE t.createdAt >= :startOfMonth AND t.createdAt < :startOfNextMonth GROUP BY t.transactionType")
    List<Object[]> getMonthlyTransactionSummary(@Param("startOfMonth") LocalDateTime startOfMonth, @Param("startOfNextMonth") LocalDateTime startOfNextMonth);
    
    /**
     * Find pending transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' ORDER BY t.createdAt ASC")
    List<Transaction> findPendingTransactions();
}