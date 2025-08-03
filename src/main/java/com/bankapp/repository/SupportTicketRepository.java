package com.bankapp.repository;

import com.bankapp.entity.SupportTicket;
import com.bankapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    List<SupportTicket> findByUserOrderByCreatedAtDesc(User user);

    Page<SupportTicket> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<SupportTicket> findByStatusOrderByCreatedAtDesc(SupportTicket.TicketStatus status);

    Page<SupportTicket> findByStatusOrderByCreatedAtDesc(SupportTicket.TicketStatus status, Pageable pageable);

    List<SupportTicket> findByPriorityOrderByCreatedAtDesc(SupportTicket.Priority priority);

    List<SupportTicket> findByCategoryOrderByCreatedAtDesc(SupportTicket.Category category);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.user = :user AND t.status = :status")
    long countByUserAndStatus(@Param("user") User user, @Param("status") SupportTicket.TicketStatus status);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = :status")
    long countByStatus(@Param("status") SupportTicket.TicketStatus status);

    @Query("SELECT t FROM SupportTicket t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<SupportTicket> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM SupportTicket t WHERE t.user = :user AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<SupportTicket> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM SupportTicket t WHERE t.status IN ('OPEN', 'IN_PROGRESS') ORDER BY t.priority DESC, t.createdAt ASC")
    List<SupportTicket> findActiveTicketsOrderByPriorityAndDate();

    @Query("SELECT t FROM SupportTicket t WHERE " +
           "t.ticketNumber LIKE CONCAT('%', :searchTerm, '%') OR " +
           "LOWER(t.subject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY t.createdAt DESC")
    Page<SupportTicket> searchTickets(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Statistics queries
    @Query("SELECT t.category, COUNT(t) FROM SupportTicket t GROUP BY t.category ORDER BY COUNT(t) DESC")
    List<Object[]> getTicketCountByCategory();

    @Query("SELECT t.priority, COUNT(t) FROM SupportTicket t WHERE t.status IN ('OPEN', 'IN_PROGRESS') GROUP BY t.priority")
    List<Object[]> getActiveTicketCountByPriority();

    @Query("SELECT DATE(t.createdAt) as date, COUNT(t) as count FROM SupportTicket t " +
           "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(t.createdAt) ORDER BY DATE(t.createdAt) DESC")
    List<Object[]> getDailyTicketCount(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}