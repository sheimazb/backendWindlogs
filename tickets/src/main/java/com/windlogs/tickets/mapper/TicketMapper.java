package com.windlogs.tickets.mapper;

import com.windlogs.tickets.dto.TicketDTO;
import com.windlogs.tickets.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TicketMapper{
    TicketMapper INSTANCE = Mappers.getMapper(TicketMapper.class);

    TicketDTO toDTO(Ticket ticket);

    Ticket toEntity(TicketDTO ticketDTO);
}
