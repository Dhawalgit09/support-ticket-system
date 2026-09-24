package com.supportticket.controller;

import com.supportticket.dto.request.CreateTicketRequest;
import com.supportticket.dto.request.UpdateTicketRequest;
import com.supportticket.dto.request.UpdateTicketStatusRequest;
import com.supportticket.dto.response.TicketDetailResponse;
import com.supportticket.dto.response.TicketSummaryResponse;
import com.supportticket.entity.TicketStatus;
import com.supportticket.service.TicketService;
import com.supportticket.service.status.TicketStatusService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketStatusService ticketStatusService;

    public TicketController(TicketService ticketService, TicketStatusService ticketStatusService) {
        this.ticketService = ticketService;
        this.ticketStatusService = ticketStatusService;
    }

    @PostMapping
    public ResponseEntity<TicketDetailResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketDetailResponse created = ticketService.create(request);
        URI location = URI.create("/api/tickets/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<TicketSummaryResponse> listTickets(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TicketStatus status) {
        return ticketService.listTickets(search, status);
    }

    @GetMapping("/{id}")
    public TicketDetailResponse getTicket(@PathVariable Long id) {
        return ticketService.getById(id);
    }

    @PatchMapping("/{id}")
    public TicketDetailResponse updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request) {
        return ticketService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public TicketDetailResponse updateTicketStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request) {
        return ticketStatusService.transitionStatus(id, request.status());
    }
}
