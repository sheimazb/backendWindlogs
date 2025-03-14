package com.windlogs.tickets.entity;

import com.windlogs.tickets.enums.Priority;
import com.windlogs.tickets.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Enumerated(EnumType.STRING)
    private Status status;

    @ElementCollection
    private List<String> attachments;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private String assignedTo;

    private String tenant;
    
    private Long userId;
    
    private String userEmail;
}
