package com.windlogs.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class statsResponse {
    private long totalPartners;
    private long activePartners;
    private long lockedPartners;
}

