package com.bankapp.controller;

import com.bankapp.entity.SupportTicket;
import com.bankapp.entity.User;
import com.bankapp.security.CustomUserDetailsService;
import com.bankapp.service.SupportTicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/support")
public class SupportController {

    @Autowired
    private SupportTicketService supportTicketService;

    @GetMapping
    public String supportHome(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("user", currentUser);
        
        // Get recent tickets for this user
        Pageable recentTicketsPageable = PageRequest.of(0, 5);
        Page<SupportTicket> recentTickets = supportTicketService.getUserTickets(currentUser, recentTicketsPageable);
        model.addAttribute("recentTickets", recentTickets.getContent());
        
        return "support/index";
    }

    @GetMapping("/tickets")
    public String viewTickets(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String status) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<SupportTicket> tickets;
        if (status != null && !status.isEmpty()) {
            SupportTicket.TicketStatus ticketStatus = SupportTicket.TicketStatus.valueOf(status.toUpperCase());
            // For user view, we need to filter by user AND status - we'll get all user tickets and filter in service
            tickets = supportTicketService.getUserTickets(currentUser, pageable);
        } else {
            tickets = supportTicketService.getUserTickets(currentUser, pageable);
        }
        
        model.addAttribute("user", currentUser);
        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("ticketStatuses", SupportTicket.TicketStatus.values());
        
        return "support/tickets";
    }

    @GetMapping("/create")
    public String createTicketPage(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("user", currentUser);
        model.addAttribute("ticketRequest", new TicketRequest());
        model.addAttribute("categories", SupportTicket.Category.values());
        model.addAttribute("priorities", SupportTicket.Priority.values());
        
        return "support/create";
    }

    @PostMapping("/create")
    public String createTicket(@Valid @ModelAttribute("ticketRequest") TicketRequest request,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            User currentUser = getCurrentUser();
            model.addAttribute("user", currentUser);
            model.addAttribute("categories", SupportTicket.Category.values());
            model.addAttribute("priorities", SupportTicket.Priority.values());
            return "support/create";
        }

        try {
            User currentUser = getCurrentUser();
            SupportTicket ticket = supportTicketService.createTicket(
                currentUser,
                request.getSubject(),
                request.getDescription(),
                request.getCategory(),
                request.getPriority()
            );

            redirectAttributes.addFlashAttribute("success",
                "Support ticket created successfully! Ticket number: " + ticket.getTicketNumber());
            return "redirect:/support/tickets";
        } catch (RuntimeException e) {
            User currentUser = getCurrentUser();
            model.addAttribute("user", currentUser);
            model.addAttribute("categories", SupportTicket.Category.values());
            model.addAttribute("priorities", SupportTicket.Priority.values());
            model.addAttribute("error", e.getMessage());
            return "support/create";
        }
    }

    @GetMapping("/tickets/{id}")
    public String viewTicketDetails(@PathVariable Long id, Model model) {
        try {
            SupportTicket ticket = supportTicketService.getTicketById(id);
            User currentUser = getCurrentUser();

            // Ensure user can only view their own tickets
            if (!ticket.getUser().getId().equals(currentUser.getId())) {
                return "redirect:/support/tickets?error=unauthorized";
            }

            model.addAttribute("ticket", ticket);
            model.addAttribute("user", currentUser);
            return "support/ticket-details";
        } catch (RuntimeException e) {
            return "redirect:/support/tickets?error=notfound";
        }
    }

    @GetMapping("/faq")
    public String faqPage(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("user", currentUser);
        return "support/faq";
    }

    @GetMapping("/contact")
    public String contactPage(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("user", currentUser);
        model.addAttribute("contactRequest", new ContactRequest());
        return "support/contact";
    }

    @PostMapping("/contact")
    public String submitContactForm(@Valid @ModelAttribute("contactRequest") ContactRequest request,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            User currentUser = getCurrentUser();
            model.addAttribute("user", currentUser);
            return "support/contact";
        }

        try {
            User currentUser = getCurrentUser();
            
            // Create a support ticket from the contact form
            SupportTicket ticket = supportTicketService.createTicket(
                currentUser,
                "Contact Form: " + request.getSubject(),
                "Contact Type: " + request.getContactType() + "\n\nMessage:\n" + request.getMessage(),
                SupportTicket.Category.GENERAL_INQUIRY,
                SupportTicket.Priority.MEDIUM
            );

            redirectAttributes.addFlashAttribute("success",
                "Your message has been submitted successfully! A support ticket has been created: " + ticket.getTicketNumber());
            return "redirect:/support";
        } catch (RuntimeException e) {
            User currentUser = getCurrentUser();
            model.addAttribute("user", currentUser);
            model.addAttribute("error", e.getMessage());
            return "support/contact";
        }
    }

    // API endpoints
    @PostMapping("/api/tickets")
    @ResponseBody
    public ResponseEntity<?> apiCreateTicket(@Valid @RequestBody TicketRequest request) {
        try {
            User currentUser = getCurrentUser();
            SupportTicket ticket = supportTicketService.createTicket(
                currentUser,
                request.getSubject(),
                request.getDescription(),
                request.getCategory(),
                request.getPriority()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Support ticket created successfully");
            response.put("ticket", createTicketResponse(ticket));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/tickets")
    @ResponseBody
    public ResponseEntity<?> apiGetUserTickets(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        try {
            User currentUser = getCurrentUser();
            Pageable pageable = PageRequest.of(page, size);
            Page<SupportTicket> tickets = supportTicketService.getUserTickets(currentUser, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("tickets", tickets.getContent().stream()
                .map(this::createTicketResponse).toList());
            response.put("totalElements", tickets.getTotalElements());
            response.put("totalPages", tickets.getTotalPages());
            response.put("currentPage", page);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/tickets/{id}")
    @ResponseBody
    public ResponseEntity<?> apiGetTicketDetails(@PathVariable Long id) {
        try {
            SupportTicket ticket = supportTicketService.getTicketById(id);
            User currentUser = getCurrentUser();

            // Ensure user can only view their own tickets
            if (!ticket.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unauthorized"));
            }

            return ResponseEntity.ok(createTicketResponse(ticket));
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

    private Map<String, Object> createTicketResponse(SupportTicket ticket) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", ticket.getId());
        response.put("ticketNumber", ticket.getTicketNumber());
        response.put("subject", ticket.getSubject());
        response.put("description", ticket.getDescription());
        response.put("category", ticket.getCategory().name());
        response.put("priority", ticket.getPriority().name());
        response.put("status", ticket.getStatus().name());
        response.put("createdAt", ticket.getCreatedAt());
        response.put("updatedAt", ticket.getUpdatedAt());
        
        if (ticket.getResolution() != null) {
            response.put("resolution", ticket.getResolution());
            response.put("resolvedBy", ticket.getResolvedBy());
            response.put("resolvedAt", ticket.getResolvedAt());
        }
        
        return response;
    }

    // DTO classes
    public static class TicketRequest {
        private String subject;
        private String description;
        private SupportTicket.Category category;
        private SupportTicket.Priority priority;

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public SupportTicket.Category getCategory() { return category; }
        public void setCategory(SupportTicket.Category category) { this.category = category; }
        public SupportTicket.Priority getPriority() { return priority; }
        public void setPriority(SupportTicket.Priority priority) { this.priority = priority; }
    }

    public static class ContactRequest {
        private String subject;
        private String message;
        private String contactType;

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getContactType() { return contactType; }
        public void setContactType(String contactType) { this.contactType = contactType; }
    }
}