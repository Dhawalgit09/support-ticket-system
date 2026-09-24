package com.supportticket.dto.response;

public record FieldErrorResponse(
        String field,
        String message,
        Object rejectedValue
) {
}
