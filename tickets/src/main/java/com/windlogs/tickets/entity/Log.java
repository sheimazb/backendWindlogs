package com.windlogs.tickets.entity;

import com.windlogs.tickets.enums.LogSeverity;
import com.windlogs.tickets.enums.LogType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "log_seq")
    @SequenceGenerator(name = "log_seq", sequenceName = "log_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    private LogType type;

    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String source;

    private String errorCode;

    private String customMessage;

    @Enumerated(EnumType.STRING)
    private LogSeverity severity;

    private String tenant;

    private LocalDateTime createdAt;

    @Column(name = "project_id")
    private Long projectId;
    
    // New fields to match Fluentd format
    
    /**
     * Process ID from the log
     */
    private String pid;
    
    /**
     * Thread name from the log
     */
    private String thread;
    
    /**
     * Class name that generated the log
     */
    @Column(name = "class_name")
    private String className;
    
    /**
     * Container ID where the log was generated
     */
    @Column(name = "container_id")
    private String containerId;
    
    /**
     * Container name where the log was generated
     */
    @Column(name = "container_name")
    private String containerName;
    
    /**
     * Original timestamp value from Fluentd
     */
    @Column(name = "original_timestamp")
    private Double originalTimestamp;

    /**
     * Tag project
     */

    @Column(name="tag")
    private String tag;

    @OneToMany(mappedBy = "log", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ticket> tickets = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = createdAt;
        }
    }
}
