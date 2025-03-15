package com.windlogs.tickets.service;

import com.windlogs.tickets.dto.SolutionDTO;
import com.windlogs.tickets.entity.Solution;
import com.windlogs.tickets.entity.Ticket;
import com.windlogs.tickets.enums.SolutionStatus;
import com.windlogs.tickets.exception.UnauthorizedException;
import com.windlogs.tickets.mapper.SolutionMapper;
import com.windlogs.tickets.repository.SolutionRepository;
import com.windlogs.tickets.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SolutionServiceTest {

    @Mock
    private SolutionRepository solutionRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SolutionMapper solutionMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private SolutionService solutionService;

    private Ticket ticket;
    private Solution solution;
    private SolutionDTO solutionDTO;
    private final Long userId = 1L;
    private final String userEmail = "test@example.com";
    private final String tenant = "test-tenant";

    @BeforeEach
    void setUp() {
        // Set up test data
        ticket = new Ticket();
        ticket.setId(1L);
        ticket.setAssignedToUserId(userId);
        ticket.setTenant(tenant);

        solution = new Solution();
        solution.setId(1L);
        solution.setTicket(ticket);
        solution.setAuthorUserId(userId);
        solution.setTenant(tenant);
        solution.setStatus(SolutionStatus.DRAFT);

        solutionDTO = new SolutionDTO();
        solutionDTO.setId(1L);
        solutionDTO.setTicketId(1L);
        solutionDTO.setAuthorUserId(userId);
        solutionDTO.setAuthorEmail(userEmail);
        solutionDTO.setStatus(SolutionStatus.DRAFT);
    }

    @Test
    void createSolution_Success() {
        // Arrange
        when(ticketRepository.findByIdAndTenant(anyLong(), anyString())).thenReturn(Optional.of(ticket));
        when(solutionRepository.findByTicketId(anyLong())).thenReturn(Optional.empty());
        when(solutionMapper.toEntity(any(SolutionDTO.class))).thenReturn(solution);
        when(solutionRepository.save(any(Solution.class))).thenReturn(solution);
        when(solutionMapper.toDTO(any(Solution.class))).thenReturn(solutionDTO);

        // Act
        SolutionDTO result = solutionService.createSolution(solutionDTO, userId, userEmail, tenant);

        // Assert
        assertNotNull(result);
        assertEquals(solutionDTO.getId(), result.getId());
        assertEquals(solutionDTO.getTicketId(), result.getTicketId());
        assertEquals(userEmail, result.getAuthorEmail());
        verify(solutionRepository, times(1)).save(any(Solution.class));
    }

    @Test
    void createSolution_TicketNotFound() {
        // Arrange
        when(ticketRepository.findByIdAndTenant(anyLong(), anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> 
            solutionService.createSolution(solutionDTO, userId, userEmail, tenant)
        );
        verify(solutionRepository, never()).save(any(Solution.class));
    }

    @Test
    void createSolution_UserNotAssigned() {
        // Arrange
        ticket.setAssignedToUserId(2L); // Different user ID
        when(ticketRepository.findByIdAndTenant(anyLong(), anyString())).thenReturn(Optional.of(ticket));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> 
            solutionService.createSolution(solutionDTO, userId, userEmail, tenant)
        );
        verify(solutionRepository, never()).save(any(Solution.class));
    }

    @Test
    void createSolution_SolutionAlreadyExists() {
        // Arrange
        when(ticketRepository.findByIdAndTenant(anyLong(), anyString())).thenReturn(Optional.of(ticket));
        when(solutionRepository.findByTicketId(anyLong())).thenReturn(Optional.of(solution));

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> 
            solutionService.createSolution(solutionDTO, userId, userEmail, tenant)
        );
        verify(solutionRepository, never()).save(any(Solution.class));
    }

    @Test
    void getSolutionById_Success() {
        // Arrange
        when(solutionRepository.findByIdAndTenant(anyLong(), anyString())).thenReturn(Optional.of(solution));
        when(solutionMapper.toDTO(any(Solution.class))).thenReturn(solutionDTO);

        // Act
        SolutionDTO result = solutionService.getSolutionById(1L, tenant);

        // Assert
        assertNotNull(result);
        assertEquals(solutionDTO.getId(), result.getId());
    }

    @Test
    void getSolutionById_NotFound() {
        // Arrange
        when(solutionRepository.findByIdAndTenant(anyLong(), anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> 
            solutionService.getSolutionById(1L, tenant)
        );
    }

    @Test
    void updateSolution_Success() {
        // Arrange
        when(solutionRepository.findByIdAndTenant(anyLong(), anyString())).thenReturn(Optional.of(solution));
        when(solutionMapper.toEntity(any(SolutionDTO.class))).thenReturn(solution);
        when(solutionRepository.save(any(Solution.class))).thenReturn(solution);
        when(solutionMapper.toDTO(any(Solution.class))).thenReturn(solutionDTO);

        // Act
        SolutionDTO result = solutionService.updateSolution(1L, solutionDTO, userId, tenant);

        // Assert
        assertNotNull(result);
        assertEquals(solutionDTO.getId(), result.getId());
        verify(solutionRepository, times(1)).save(any(Solution.class));
    }

    @Test
    void updateSolution_NotAuthor() {
        // Arrange
        solution.setAuthorUserId(2L); // Different user ID
        when(solutionRepository.findByIdAndTenant(anyLong(), anyString())).thenReturn(Optional.of(solution));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> 
            solutionService.updateSolution(1L, solutionDTO, userId, tenant)
        );
        verify(solutionRepository, never()).save(any(Solution.class));
    }
}
