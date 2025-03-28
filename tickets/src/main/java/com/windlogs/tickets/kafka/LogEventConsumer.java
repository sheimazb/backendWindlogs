package com.windlogs.tickets.kafka;

import com.windlogs.tickets.dto.LogDTO;
import com.windlogs.tickets.dto.TicketDTO;
import com.windlogs.tickets.enums.LogSeverity;
import com.windlogs.tickets.enums.LogType;
import com.windlogs.tickets.enums.Priority;
import com.windlogs.tickets.enums.Status;
import com.windlogs.tickets.service.LogService;
import com.windlogs.tickets.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class LogEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(LogEventConsumer.class);
    
    private final LogService logService;
    private final TicketService ticketService;

    @KafkaListener(
            topics = "log-events-topic",
            groupId = "ticket-service-group"
    )
    public void consume(LogEvent event) {
        logger.info("Received log event: {}", event);
        
        try {
            // Convert timestamp from Unix timestamp to LocalDateTime
            LocalDateTime timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochSecond((long) event.timestamp()), 
                ZoneId.systemDefault()
            );

            // Create log entry
            LogDTO logDTO = new LogDTO();
            logDTO.setType(LogType.valueOf(event.level().toUpperCase())); // Convert level to LogType
            logDTO.setTimestamp(timestamp);
            logDTO.setDescription(buildDescription(event));
            logDTO.setSource(event.source());
            logDTO.setSeverity(determineSeverity(event.level()));
            logDTO.setTenant(extractTenant(event.container_name())); // Extract tenant from container name
            logDTO.setProjectId(1L); // Default project ID, adjust as needed
            logDTO.setErrorCode("E_" + event.thread()); // Use thread as part of error code
            logDTO.setCustomMessage(event.message());
            
            // Only create tickets for ERROR level logs
            if ("ERROR".equalsIgnoreCase(event.level())) {
                // Create the log first
                var createdLog = logService.createLogFromDTO(logDTO, "Bearer " + System.getenv("SERVICE_TOKEN"));
                
                // Create ticket
                TicketDTO ticketDTO = new TicketDTO();
                ticketDTO.setLogId(createdLog.getId());
                ticketDTO.setTitle(String.format("Error in %s: %s", event.source(), event.class_name()));
                ticketDTO.setDescription(buildTicketDescription(event));
                ticketDTO.setPriority(Priority.HIGH);
                ticketDTO.setStatus(Status.PENDING);
                ticketDTO.setTenant(logDTO.getTenant());
                
                // Create the ticket
                ticketService.createTicket(ticketDTO);
                logger.info("Created ticket for log ID: {}", createdLog.getId());
            } else {
                // Just create the log without a ticket
                logService.createLogFromDTO(logDTO, "Bearer " + System.getenv("SERVICE_TOKEN"));
            }
        } catch (Exception e) {
            logger.error("Error processing log event: {}", e.getMessage(), e);
        }
    }

    private String buildDescription(LogEvent event) {
        return String.format(
            "Error in class %s (Thread: %s, PID: %s)\nContainer: %s\nMessage: %s",
            event.class_name(),
            event.thread(),
            event.pid(),
            event.container_name(),
            event.message()
        );
    }

    private String buildTicketDescription(LogEvent event) {
        return String.format(
            """
            Error Details:
            - Class: %s
            - Thread: %s
            - PID: %s
            - Container: %s
            - Container ID: %s
            - Message: %s
            - Source: %s
            """,
            event.class_name(),
            event.thread(),
            event.pid(),
            event.container_name(),
            event.container_id(),
            event.message(),
            event.source()
        );
    }

    private LogSeverity determineSeverity(String level) {
        return switch (level.toUpperCase()) {
            case "ERROR" -> LogSeverity.HIGH;
            case "WARN" -> LogSeverity.MEDIUM;
            default -> LogSeverity.LOW;
        };
    }

    private String extractTenant(String containerName) {
        // Extract tenant from container name, e.g., "/proj1_container" -> "proj1"
        if (containerName != null && containerName.startsWith("/")) {
            String[] parts = containerName.substring(1).split("_");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return "default";
    }
} 