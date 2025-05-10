package com.windlogs.tickets.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class ActivityStatsDTO {
    private Date day;
    private Long count;

    public ActivityStatsDTO(Date day, Long count) {
        this.day = day;
        this.count = count;
    }
}
