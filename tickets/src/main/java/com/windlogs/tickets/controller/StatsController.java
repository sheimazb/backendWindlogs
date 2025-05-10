package com.windlogs.tickets.controller;

import com.windlogs.tickets.dto.ActivityStatsDTO;
import com.windlogs.tickets.dto.LogStatsDTO;
import com.windlogs.tickets.repository.LogRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@AllArgsConstructor
public class StatsController {

    private final LogRepository logRepository;

    @GetMapping("/errors-by-day/project/{projectId}")
    public List<LogStatsDTO> getErrorsByDayForProject(@PathVariable Long projectId) {
        return logRepository.getLogStatsByDayForProject(projectId);
    }

    @GetMapping("/activity-by-day/project/{projectId}")
    public List<ActivityStatsDTO> getActivitiesByDayForProject(@PathVariable Long projectId) {
        return logRepository.getActivityByDayForProject(projectId);
    }

}
