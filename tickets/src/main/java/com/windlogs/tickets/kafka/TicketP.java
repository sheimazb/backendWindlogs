package com.windlogs.tickets.kafka;

import com.windlogs.tickets.enums.Status;

public record TicketP(
        Status status,
        String userEmail,
        String title,
        String description,
        String tenant
) {

}
