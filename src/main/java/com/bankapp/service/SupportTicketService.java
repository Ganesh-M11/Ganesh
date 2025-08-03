package com.bankapp.service;

import com.bankapp.entity.SupportTicket;
import com.bankapp.entity.User;
import com.bankapp.repository.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SupportTicketService {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private NotificationService notificationService;

    public SupportTicket createTicket(User user, String subject, String description, SupportTicket.Category category, SupportTicket.Priority priority) {
        SupportTicket ticket = new SupportTicket(subject, description, category, user);
        ticket.setPriority(priority != null ? priority : SupportTicket.Priority.MEDIUM);
        ticket.setTicketNumber(generateTicketNumber());
        
        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        
        // Notify user that ticket was created
        notificationService.createSystemNotification(user, 
            "Support Ticket Created", 
            "Your support ticket #" + savedTicket.getTicketNumber() + " has been created and will be reviewed by our support team.");
        
        return savedTicket;
    }

    public SupportTicket getTicketById(Long id) {
        return supportTicketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Support ticket not found"));
    }

    public SupportTicket getTicketByNumber(String ticketNumber) {
        return supportTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Support ticket not found"));
    }

    public List<SupportTicket> getUserTickets(User user) {
        return supportTicketRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Page<SupportTicket> getUserTickets(User user, Pageable pageable) {
        return supportTicketRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public void updateTicketStatus(Long ticketId, SupportTicket.TicketStatus status, String updatedBy) {
        SupportTicket ticket = getTicketById(ticketId);
        ticket.setStatus(status);
        supportTicketRepository.save(ticket);
        
        // Notify user of status change
        String statusMessage = "Your support ticket #" + ticket.getTicketNumber() + " status has been updated to: " + status.getDisplayName();
        notificationService.createSystemNotification(ticket.getUser(), "Ticket Status Update", statusMessage);
    }

    public void resolveTicket(Long ticketId, String resolution, String resolvedBy) {
        SupportTicket ticket = getTicketById(ticketId);
        ticket.resolve(resolution, resolvedBy);
        supportTicketRepository.save(ticket);
        
        // Notify user that ticket is resolved
        notificationService.createSystemNotification(ticket.getUser(), 
            "Ticket Resolved", 
            "Your support ticket #" + ticket.getTicketNumber() + " has been resolved. Resolution: " + resolution);
    }

    public void closeTicket(Long ticketId) {
        SupportTicket ticket = getTicketById(ticketId);
        ticket.close();
        supportTicketRepository.save(ticket);
        
        // Notify user that ticket is closed
        notificationService.createSystemNotification(ticket.getUser(), 
            "Ticket Closed", 
            "Your support ticket #" + ticket.getTicketNumber() + " has been closed.");
    }

    // Admin methods
    public Page<SupportTicket> getAllTickets(Pageable pageable) {
        return supportTicketRepository.findAll(pageable);
    }

    public Page<SupportTicket> getTicketsByStatus(SupportTicket.TicketStatus status, Pageable pageable) {
        return supportTicketRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    public List<SupportTicket> getActiveTickets() {
        return supportTicketRepository.findActiveTicketsOrderByPriorityAndDate();
    }

    public Page<SupportTicket> searchTickets(String searchTerm, Pageable pageable) {
        return supportTicketRepository.searchTickets(searchTerm, pageable);
    }

    public long getTicketCountByStatus(SupportTicket.TicketStatus status) {
        return supportTicketRepository.countByStatus(status);
    }

    public List<Object[]> getTicketCountByCategory() {
        return supportTicketRepository.getTicketCountByCategory();
    }

    public List<Object[]> getActiveTicketCountByPriority() {
        return supportTicketRepository.getActiveTicketCountByPriority();
    }

    public List<Object[]> getDailyTicketCount(LocalDateTime startDate, LocalDateTime endDate) {
        return supportTicketRepository.getDailyTicketCount(startDate, endDate);
    }

    private String generateTicketNumber() {
        String ticketNumber;
        do {
            ticketNumber = "TKT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        } while (supportTicketRepository.findByTicketNumber(ticketNumber).isPresent());
        
        return ticketNumber;
    }
}