package com.windlogs.tickets.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.Data;
import java.util.Map;
import java.util.List;

@Slf4j
@Service
public class ExceptionAnalyzerService {

    @Value("${exception.analyzer.api.url:http://localhost:8000}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public ExceptionAnalyzerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Data
    private static class ExceptionAnalyzerRequest {
        private String message;
        private String model = "gemini"; // Default to Gemini model
    }

    @Data
    private static class StackTraceAnalysisRequest {
        private String message;
    }

    @Data
    public static class StackTraceAnalysisResponse {
        @JsonProperty("exception_type")
        private String exceptionType;
        @JsonProperty("stack_trace")
        private String stackTrace;
        private Map<String, Object> analysis;
        private String error;
    }

    @Data
    private static class ExceptionAnalyzerResponse {
        @JsonProperty("exception_type")
        private String exceptionType;
        private float score;
        private String matchedFrom;
        @JsonProperty("model_used")
        private String modelUsed;
    }

    @Data
    private static class LogRecommendationRequest {
        @JsonProperty("log_message")
        private String logMessage;
        private Integer k;
    }

    /**
     * Custom converter to handle "nan" values for Long fields
     */
    public static class SafeLongDeserializer extends StdConverter<String, Long> {
        @Override
        public Long convert(String value) {
            if (value == null || value.isEmpty() || "nan".equalsIgnoreCase(value) || "null".equalsIgnoreCase(value)) {
                return null;
            }
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    @Data
    public static class SimilarLog {
        @JsonProperty("log_id")
        private Long logId;
        
        @JsonProperty("log_message")
        private String logMessage;
        
        @JsonProperty("ticket_id")
        private Long ticketId;
        
        @JsonProperty("error_type")
        private String errorType;
        
        @JsonProperty("detected_types")
        private List<String> detectedTypes;
        
        @JsonProperty("exception_types")
        private List<String> exceptionTypes;
        
        @JsonProperty("solution_title")
        private String solutionTitle;
        
        @JsonProperty("solution_content")
        private String solutionContent;
            
        @JsonProperty("solution_author_user_id")
        @JsonDeserialize(converter = SafeLongDeserializer.class)
        private Long solutionAuthorUserId;

        private String severity;
        private String similarity;
        
        @JsonProperty("term_match")
        private String termMatch;
        
        @JsonProperty("match_details")
        private String matchDetails;
    }

    @Data
    public static class QueryAnalysis {
        @JsonProperty("detected_types")
        private List<String> detectedTypes;
        
        private String severity;
        
        @JsonProperty("error_code")
        private String errorCode;
        
        @JsonProperty("exception_types")
        private List<String> exceptionTypes;
        
        @JsonProperty("term_count")
        private Integer termCount;
        
        @JsonProperty("normalized_query")
        private String normalizedQuery;
    }

    @Data
    public static class LogRecommendationResponse {
        @JsonProperty("similar_logs")
        private List<SimilarLog> similarLogs;
        
        private String message;
        
        @JsonProperty("query_analysis")
        private QueryAnalysis queryAnalysis;
    }

    public String analyzeException(String logMessage) {
        try {
            log.debug("Analyzing exception message: {}", logMessage.substring(0, Math.min(logMessage.length(), 100)));

            String endpoint = apiUrl + "/classify-log";

            ExceptionAnalyzerRequest request = new ExceptionAnalyzerRequest();
            request.setMessage(logMessage);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ExceptionAnalyzerRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ExceptionAnalyzerResponse> responseEntity = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    ExceptionAnalyzerResponse.class
            );

            ExceptionAnalyzerResponse response = responseEntity.getBody();

            if (response != null && response.getExceptionType() != null) {
                log.info("Exception analyzed: {} (using {})", response.getExceptionType(), response.getModelUsed());
                return response.getExceptionType();
            }

            log.warn("Received null response from analyzer API");
            return "UnknownException";

        } catch (Exception e) {
            log.error("Failed to analyze exception: {}", e.getMessage(), e);
            return "AnalysisFailedException: " + e.getMessage();
        }
    }

    public StackTraceAnalysisResponse analyzeStackTrace(String logMessage) {
        try {
            log.debug("Analyzing stack trace for message: {}", logMessage.substring(0, Math.min(logMessage.length(), 100)));

            String endpoint = apiUrl + "/analyze-stack-trace";

            StackTraceAnalysisRequest request = new StackTraceAnalysisRequest();
            request.setMessage(logMessage);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<StackTraceAnalysisRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<StackTraceAnalysisResponse> responseEntity = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    StackTraceAnalysisResponse.class
            );

            StackTraceAnalysisResponse response = responseEntity.getBody();

            if (response != null) {
                log.info("Stack trace analyzed successfully. Found exception type: {}", response.getExceptionType());
                return response;
            }

            log.warn("Received null response from stack trace analyzer API");
            StackTraceAnalysisResponse errorResponse = new StackTraceAnalysisResponse();
            errorResponse.setError("Received null response from API");
            return errorResponse;

        } catch (Exception e) {
            log.error("Failed to analyze stack trace", e);
            StackTraceAnalysisResponse errorResponse = new StackTraceAnalysisResponse();
            errorResponse.setError("Analysis failed: " + e.getMessage());
            return errorResponse;
        }
    }
    
    public LogRecommendationResponse recommendSolutions(String logMessage, Integer k) {
        try {
            log.debug("Recommending solutions for log message: {}", logMessage.substring(0, Math.min(logMessage.length(), 100)));

            String endpoint = apiUrl + "/recommend-solutions";

            LogRecommendationRequest request = new LogRecommendationRequest();
            request.setLogMessage(logMessage);
            request.setK(k);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<LogRecommendationRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<LogRecommendationResponse> responseEntity = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    LogRecommendationResponse.class
            );

            LogRecommendationResponse response = responseEntity.getBody();

            if (response != null) {
                log.info("Log recommendations retrieved successfully. Found {} similar logs", 
                        response.getSimilarLogs() != null ? response.getSimilarLogs().size() : 0);
                return response;
            }

            log.warn("Received null response from recommendation API");
            LogRecommendationResponse errorResponse = new LogRecommendationResponse();
            errorResponse.setMessage("Received null response from API");
            return errorResponse;

        } catch (Exception e) {
            log.error("Failed to recommend solutions for log message", e);
            LogRecommendationResponse errorResponse = new LogRecommendationResponse();
            errorResponse.setMessage("Recommendation failed: " + e.getMessage());
            return errorResponse;
        }
    }
}