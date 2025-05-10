package com.windlogs.tickets.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.windlogs.tickets.service.ExceptionAnalyzerService;
import com.windlogs.tickets.service.ExceptionAnalyzerService.LogRecommendationResponse;

/**
 * Contrôleur pour le service de recommandation de solutions basées sur les logs d'erreur
 */
@RestController
@RequestMapping("/api/v1/solutions")
@RequiredArgsConstructor
@Slf4j
public class LogRecommendationController {
    
    private final ExceptionAnalyzerService exceptionAnalyzerService;

    @PostMapping("/recommendations")
    public ResponseEntity<LogRecommendationResponse> getRecommendations(@RequestBody LogAnalysisRequest request) {
        log.info("Recherche de recommandations pour un log (taille: {} caractères)", 
                request.getLogMessage() != null ? request.getLogMessage().length() : 0);
        
        int k = request.getK() != null ? request.getK() : 5;
        
        LogRecommendationResponse response = exceptionAnalyzerService.recommendSolutions(
            request.getLogMessage(), k
        );
        
        log.info("Récupération de {} recommandations similaires", 
                response.getSimilarLogs() != null ? response.getSimilarLogs().size() : 0);
        
        return ResponseEntity.ok(response);
    }
    
    @Data
    public static class LogAnalysisRequest {
        private String logMessage;
        private Integer k;
    }
} 