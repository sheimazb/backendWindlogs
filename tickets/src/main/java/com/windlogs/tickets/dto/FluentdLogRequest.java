package com.windlogs.tickets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FluentdLogRequest {
    // Basic log fields
    private String level;
    private String message;
    private String source;
    private long timestamp;
    // System identification fields
    private String pid;
    private String thread;
    private String class_name;
    private String container_id;
    private String container_name;
    private String stackTrace;
    private String exceptionType;
    private String tag;
    private String analysis;
    // Exception information
    private ThrownInfo thrown;
}