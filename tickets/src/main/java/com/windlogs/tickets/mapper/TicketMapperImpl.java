package com.windlogs.tickets.mapper;

import com.windlogs.tickets.dto.TicketDTO;
import com.windlogs.tickets.entity.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TicketMapperImpl implements TicketMapper {
    private static final Logger logger = LoggerFactory.getLogger(TicketMapperImpl.class);

    @Override
    public TicketDTO toDTO(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        TicketDTO ticketDTO = new TicketDTO();
        ticketDTO.setId(ticket.getId());
        ticketDTO.setStatus(ticket.getStatus());
        ticketDTO.setAttachments(ticket.getAttachments());
        ticketDTO.setPriority(ticket.getPriority());
        ticketDTO.setTitle(ticket.getTitle());
        ticketDTO.setDescription(ticket.getDescription());

        // Set assignedTo if present
        if (ticket.getAssignedToUserId() != null) {
            ticketDTO.setAssignedToUserId(Long.valueOf(ticket.getAssignedToUserId().toString()));
        }
        
        // Ensure tenant is set - this is critical
        String tenant = ticket.getTenant();
        if (tenant == null || tenant.isEmpty()) {
            logger.warn("Ticket entity has null or empty tenant during mapping to DTO");
        }
        ticketDTO.setTenant(tenant);
        
        // Set user information
        ticketDTO.setCreatorUserId(ticket.getCreatorUserId());
        ticketDTO.setUserEmail(ticket.getUserEmail());
        
        // Set log ID if present
        if (ticket.getLog() != null) {
            ticketDTO.setLogId(ticket.getLog().getId());
        }
        
        logger.debug("Mapped Ticket to TicketDTO - ID: {}, Tenant: {}, LogId: {}", 
                ticketDTO.getId(), ticketDTO.getTenant(), ticketDTO.getLogId());
        
        return ticketDTO;
    }

    @Override
    public Ticket toEntity(TicketDTO ticketDTO) {
        if (ticketDTO == null) {
            return null;
        }

        Ticket ticket = new Ticket();
        ticket.setId(ticketDTO.getId());
        ticket.setStatus(ticketDTO.getStatus());
        ticket.setAttachments(ticketDTO.getAttachments());
        ticket.setPriority(ticketDTO.getPriority());
        ticket.setTitle(ticketDTO.getTitle());
        ticket.setDescription(ticketDTO.getDescription());

        // Set assignedToUserId if present
        if (ticketDTO.getAssignedToUserId() != null) {
            try {
                ticket.setAssignedToUserId(Long.parseLong(String.valueOf(ticketDTO.getAssignedToUserId())));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse assignedTo as Long: {}", ticketDTO.getAssignedToUserId());
            }
        }
        
        // Ensure tenant is set - this is critical
        String tenant = ticketDTO.getTenant();
        if (tenant == null || tenant.isEmpty()) {
            logger.warn("TicketDTO has null or empty tenant during mapping to entity");
        }
        ticket.setTenant(tenant);
        
        // Set user information
        ticket.setCreatorUserId(ticketDTO.getCreatorUserId());
        ticket.setUserEmail(ticketDTO.getUserEmail());
        
        // Note: Log entity will be set by the service
        
        logger.debug("Mapped TicketDTO to Ticket - ID: {}, Tenant: {}, LogId: {}", 
                ticket.getId(), ticket.getTenant(), ticketDTO.getLogId());
        
        return ticket;
    }
}
