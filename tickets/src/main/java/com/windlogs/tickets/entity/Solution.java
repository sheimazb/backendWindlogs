package com.windlogs.tickets.entity;

import com.windlogs.tickets.enums.ComplexityLevel;
import com.windlogs.tickets.enums.SolutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "solutions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Solution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private ComplexityLevel complexity;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_user_id")
    private Long authorUserId;

    @Enumerated(EnumType.STRING)
    private SolutionStatus status;

    private Integer estimatedTime;

    private Double costEstimation;

    private String category;

    private String tenant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

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
