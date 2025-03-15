package com.windlogs.tickets.mapper;

import com.windlogs.tickets.dto.TicketDTO;
import com.windlogs.tickets.entity.Ticket;

public interface TicketMapper{
    TicketDTO toDTO(Ticket ticket);

    Ticket toEntity(TicketDTO ticketDTO);
}
