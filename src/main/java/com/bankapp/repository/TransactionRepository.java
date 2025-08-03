package com.bankapp.repository;

import com.bankapp.entity.Transaction;
import com.bankapp.entity.User;
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

    List<Transaction> findByUserOrderByCreatedAtDesc(User user);

    Page<Transaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Transaction> findByUserAndTransactionTypeOrderByCreatedAtDesc(User user, Transaction.TransactionType transactionType);

    Page<Transaction> findByUserAndTransactionTypeOrderByCreatedAtDesc(User user, Transaction.TransactionType transactionType, Pageable pageable);

    List<Transaction> findByUserAndStatusOrderByCreatedAtDesc(User user, Transaction.TransactionStatus status);

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.amount >= :minAmount AND t.amount <= :maxAmount ORDER BY t.createdAt DESC")
    List<Transaction> findByUserAndAmountRange(
            @Param("user") User user,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount
    );

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.transactionType = :type AND t.status = 'COMPLETED'")
    BigDecimal sumAmountByUserAndType(@Param("user") User user, @Param("type") Transaction.TransactionType type);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.transactionType = :type AND t.status = 'COMPLETED' AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserAndTypeAndDateRange(
            @Param("user") User user,
            @Param("type") Transaction.TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user = :user AND t.status = 'COMPLETED'")
    long countCompletedTransactionsByUser(@Param("user") User user);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'PENDING'")
    long countPendingTransactions();

    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' ORDER BY t.createdAt ASC")
    List<Transaction> findPendingTransactions();

    @Query("SELECT t FROM Transaction t WHERE t.amount > :threshold AND t.status = 'COMPLETED' ORDER BY t.createdAt DESC")
    List<Transaction> findLargeTransactions(@Param("threshold") BigDecimal threshold);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findTransactionsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Monthly transaction summary
    @Query("SELECT YEAR(t.createdAt) as year, MONTH(t.createdAt) as month, COUNT(t) as count, SUM(t.amount) as total " +
           "FROM Transaction t WHERE t.status = 'COMPLETED' " +
           "GROUP BY YEAR(t.createdAt), MONTH(t.createdAt) " +
           "ORDER BY YEAR(t.createdAt) DESC, MONTH(t.createdAt) DESC")
    List<Object[]> getMonthlyTransactionSummary();

    // Daily transaction summary for a user
    @Query("SELECT DATE(t.createdAt) as date, COUNT(t) as count, SUM(t.amount) as total " +
           "FROM Transaction t WHERE t.user = :user AND t.status = 'COMPLETED' " +
           "AND t.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(t.createdAt) " +
           "ORDER BY DATE(t.createdAt) DESC")
    List<Object[]> getDailyTransactionSummaryForUser(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.referenceNumber LIKE CONCAT('%', :searchTerm, '%') OR " +
           "t.description LIKE CONCAT('%', :searchTerm, '%') OR " +
           "t.recipientName LIKE CONCAT('%', :searchTerm, '%') OR " +
           "t.recipientAccountNumber LIKE CONCAT('%', :searchTerm, '%') " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> searchTransactions(@Param("searchTerm") String searchTerm, Pageable pageable);
}