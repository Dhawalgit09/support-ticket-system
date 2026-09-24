package com.supportticket.dto.response;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String body,
        String author,
        Instant createdAt
) {
}
