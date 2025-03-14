package com.windlogs.tickets.service;

import com.windlogs.tickets.dto.TicketDTO;
import com.windlogs.tickets.entity.Ticket;
import com.windlogs.tickets.mapper.TicketMapper;
import com.windlogs.tickets.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;

    public TicketService(TicketRepository ticketRepository, TicketMapper ticketMapper) {
        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
    }

    // Create a new ticket
    public TicketDTO createTicket(TicketDTO ticketDTO) {
        Ticket ticket = ticketMapper.toEntity(ticketDTO);
        Ticket savedTicket = ticketRepository.save(ticket);
        return ticketMapper.toDTO(savedTicket);
    }

    // Get all tickets
    public List<TicketDTO> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(ticketMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Get ticket by ID
    public TicketDTO getTicketById(Long id) {
        Optional<Ticket> ticket = ticketRepository.findById(id);
        return ticket.map(ticketMapper::toDTO).orElse(null);
    }

    // Update ticket
    public TicketDTO updateTicket(Long id, TicketDTO ticketDTO) {
        if (ticketRepository.existsById(id)) {
            Ticket ticket = ticketMapper.toEntity(ticketDTO);
            ticket.setId(id); // Ensure ID remains the same
            Ticket updatedTicket = ticketRepository.save(ticket);
            return ticketMapper.toDTO(updatedTicket);
        }
        return null;
    }

    // Delete ticket
    public boolean deleteTicket(Long id) {
        if (ticketRepository.existsById(id)) {
            ticketRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
