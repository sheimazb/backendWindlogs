package com.windlogs.authentication.controller;

import com.windlogs.authentication.dto.statsResponse;
import com.windlogs.authentication.service.StatsService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/partners")
    public statsResponse getPartnersStats() {
        return new statsResponse(statsService.totalPartners(), statsService.activePartners(), statsService.lockedPartners());
    }
 
    
 
}
