package com.windlogs.tickets.entity;

import com.windlogs.tickets.enums.Priority;
import com.windlogs.tickets.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description ;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ElementCollection
    private List<String> attachments;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;

    /**
     * The tenant identifier for this ticket.
     * This field is critical for multi-tenancy and must always be set.
     * It represents the organization/tenant that the ticket belongs to,
     * and is inherited from the manager who creates the ticket.
     */
    @Column(name = "tenant", nullable = false)
    private String tenant;
    
    @Column(name = "creator_user_id")
    private Long creatorUserId;
    
    private String userEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id")
    private Log log;

    @OneToOne(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private Solution solution;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
