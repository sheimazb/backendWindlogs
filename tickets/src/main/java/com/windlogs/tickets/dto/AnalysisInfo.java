package com.windlogs.tickets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisInfo {
    @JsonProperty("root_exception")
    private String rootException;
    private String cause;
    private String location;
    @JsonProperty("exception_chain")
    private String exceptionChain;
    private ThrownInfo recommendation;
}
