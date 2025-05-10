package com.windlogs.tickets.dto;

import lombok.Data;
import java.util.Date;

@Data
public class LogStatsDTO {
    private Date day;
    private String type;
    private long count;

    public LogStatsDTO(Date day, String type, long count) {
        this.day = day;
        this.type = type;
        this.count = count;
    }
}
