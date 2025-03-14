package com.windlogs.tickets.entity;

import com.windlogs.tickets.enums.LogSeverity;
import com.windlogs.tickets.enums.LogType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private LogType type;

    private LocalDateTime timestamp;

    private String description;

    private String source;

    private String errorCode;

    private String customMessage;

    @Enumerated(EnumType.STRING)
    private LogSeverity severity;

    private LocalDateTime createdAt;
}
