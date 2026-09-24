package com.supportticket.dto.response;

import com.supportticket.entity.TicketPriority;
import com.supportticket.entity.TicketStatus;

import java.time.Instant;

public record TicketSummaryResponse(
        Long id,
        String title,
        TicketStatus status,
        TicketPriority priority,
        String assignee,
        Instant updatedAt
) {
}
