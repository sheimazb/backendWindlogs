package com.windlogs.tickets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThrownInfo {
    private String name;
    private String message;
    private String localizedMessage;
    private String extendedStackTrace;
    private ThrownInfo cause;
} 