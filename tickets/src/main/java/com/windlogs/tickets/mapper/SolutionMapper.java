package com.windlogs.tickets.mapper;

import com.windlogs.tickets.dto.SolutionDTO;
import com.windlogs.tickets.entity.Solution;

public interface SolutionMapper {
    SolutionDTO toDTO(Solution solution);

    Solution toEntity(SolutionDTO solutionDTO);
}
