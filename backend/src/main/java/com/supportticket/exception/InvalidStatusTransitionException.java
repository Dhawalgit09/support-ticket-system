package com.supportticket.exception;

import com.supportticket.entity.TicketStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(TicketStatus current, TicketStatus requested) {
        super("Cannot transition from " + current + " to " + requested);
    }
}
