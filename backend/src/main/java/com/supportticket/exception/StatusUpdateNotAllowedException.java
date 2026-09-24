package com.supportticket.exception;

public class StatusUpdateNotAllowedException extends RuntimeException {

    public StatusUpdateNotAllowedException() {
        super("Status cannot be updated via this endpoint. Use PATCH /api/tickets/{id}/status");
    }
}
