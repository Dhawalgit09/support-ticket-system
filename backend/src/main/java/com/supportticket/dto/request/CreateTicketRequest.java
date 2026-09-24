package com.supportticket.dto.request;

import com.supportticket.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String description,
        TicketPriority priority,
        @Size(max = 100) String assignee
) {
}
