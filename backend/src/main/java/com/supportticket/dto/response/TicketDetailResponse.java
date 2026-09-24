package com.supportticket.dto.response;

import com.supportticket.entity.TicketPriority;
import com.supportticket.entity.TicketStatus;

import java.time.Instant;
import java.util.List;

public record TicketDetailResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String assignee,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> comments
) {
}
