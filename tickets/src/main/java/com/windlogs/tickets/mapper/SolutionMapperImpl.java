package com.windlogs.tickets.mapper;

import com.windlogs.tickets.dto.SolutionDTO;
import com.windlogs.tickets.entity.Solution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SolutionMapperImpl implements SolutionMapper {
    private static final Logger logger = LoggerFactory.getLogger(SolutionMapperImpl.class);

    @Override
    public SolutionDTO toDTO(Solution solution) {
        if (solution == null) {
            return null;
        }

        SolutionDTO solutionDTO = new SolutionDTO();
        solutionDTO.setId(solution.getId());
        solutionDTO.setTitle(solution.getTitle());
        solutionDTO.setComplexity(solution.getComplexity());
        solutionDTO.setContent(solution.getContent());
        solutionDTO.setAuthorUserId(solution.getAuthorUserId());
        solutionDTO.setStatus(solution.getStatus());
        solutionDTO.setEstimatedTime(solution.getEstimatedTime());
        solutionDTO.setCostEstimation(solution.getCostEstimation());
        solutionDTO.setCategory(solution.getCategory());
        solutionDTO.setTenant(solution.getTenant());
        solutionDTO.setCreatedAt(solution.getCreatedAt());
        solutionDTO.setUpdatedAt(solution.getUpdatedAt());
        
        // Set ticket ID if present
        if (solution.getTicket() != null) {
            solutionDTO.setTicketId(solution.getTicket().getId());
        }
        
        logger.debug("Mapped Solution to SolutionDTO - ID: {}, Tenant: {}, TicketId: {}", 
                solutionDTO.getId(), solutionDTO.getTenant(), solutionDTO.getTicketId());
        
        return solutionDTO;
    }

    @Override
    public Solution toEntity(SolutionDTO solutionDTO) {
        if (solutionDTO == null) {
            return null;
        }

        Solution solution = new Solution();
        solution.setId(solutionDTO.getId());
        solution.setTitle(solutionDTO.getTitle());
        solution.setComplexity(solutionDTO.getComplexity());
        solution.setContent(solutionDTO.getContent());
        solution.setAuthorUserId(solutionDTO.getAuthorUserId());
        solution.setStatus(solutionDTO.getStatus());
        solution.setEstimatedTime(solutionDTO.getEstimatedTime());
        solution.setCostEstimation(solutionDTO.getCostEstimation());
        solution.setCategory(solutionDTO.getCategory());
        solution.setTenant(solutionDTO.getTenant());
        solution.setCreatedAt(solutionDTO.getCreatedAt());
        solution.setUpdatedAt(solutionDTO.getUpdatedAt());
        
        // Note: Ticket entity will be set by the service
        
        logger.debug("Mapped SolutionDTO to Solution - ID: {}, Tenant: {}, TicketId: {}", 
                solution.getId(), solution.getTenant(), solutionDTO.getTicketId());
        
        return solution;
    }
}
