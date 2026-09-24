package com.supportticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportticket.dto.request.CreateTicketRequest;
import com.supportticket.dto.request.UpdateTicketRequest;
import com.supportticket.dto.response.TicketDetailResponse;
import com.supportticket.dto.response.TicketSummaryResponse;
import com.supportticket.entity.TicketPriority;
import com.supportticket.entity.TicketStatus;
import com.supportticket.exception.GlobalExceptionHandler;
import com.supportticket.exception.InvalidStatusTransitionException;
import com.supportticket.exception.StatusUpdateNotAllowedException;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.service.TicketService;
import com.supportticket.service.status.TicketStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private TicketStatusService ticketStatusService;

    @Test
    void createTicket_withValidBody_returns201AndLocationHeader() throws Exception {
        TicketDetailResponse response = sampleDetailResponse(1L);
        when(ticketService.create(any(CreateTicketRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cannot login",
                                  "description": "User reports 500 error on login page.",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tickets/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Cannot login"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.comments").isArray());
    }

    @Test
    void createTicket_withMissingTitle_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "User reports 500 error on login page."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    void createTicket_withTitleTooLong_returns400() throws Exception {
        String longTitle = "a".repeat(201);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTicketRequest(longTitle, "Valid description", null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void listTickets_returns200AndArrayShape() throws Exception {
        Instant updatedAt = Instant.parse("2026-09-24T14:30:00Z");
        when(ticketService.listTickets(null, null)).thenReturn(List.of(
                new TicketSummaryResponse(1L, "Cannot login", TicketStatus.OPEN, TicketPriority.HIGH, null, updatedAt)
        ));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Cannot login"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].updatedAt").exists());
    }

    @Test
    void getTicket_whenFound_returns200() throws Exception {
        when(ticketService.getById(1L)).thenReturn(sampleDetailResponse(1L));

        mockMvc.perform(get("/api/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("User reports 500 error on login page."));
    }

    @Test
    void getTicket_whenMissing_returns404() throws Exception {
        when(ticketService.getById(99L)).thenThrow(new TicketNotFoundException(99L));

        mockMvc.perform(get("/api/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ticket not found with id: 99"));
    }

    @Test
    void updateTicket_withValidBody_returns200() throws Exception {
        TicketDetailResponse updated = new TicketDetailResponse(
                1L,
                "Updated title",
                "User reports 500 error on login page.",
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                null,
                Instant.parse("2026-09-24T10:00:00Z"),
                Instant.parse("2026-09-24T15:00:00Z"),
                List.of()
        );
        when(ticketService.update(eq(1L), any(UpdateTicketRequest.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated title\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"));
    }

    @Test
    void updateTicket_withStatusField_returns400() throws Exception {
        when(ticketService.update(eq(1L), any(UpdateTicketRequest.class)))
                .thenThrow(new StatusUpdateNotAllowedException());

        mockMvc.perform(patch("/api/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Status cannot be updated via this endpoint. Use PATCH /api/tickets/{id}/status"));
    }

    @Test
    void listTickets_withInvalidStatusQueryParam_returns400() throws Exception {
        mockMvc.perform(get("/api/tickets").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'"));
    }

    @Test
    void updateTicketStatus_withValidTransition_returns200() throws Exception {
        TicketDetailResponse response = new TicketDetailResponse(
                1L,
                "Cannot login",
                "User reports 500 error on login page.",
                TicketStatus.IN_PROGRESS,
                TicketPriority.HIGH,
                null,
                Instant.parse("2026-09-24T10:00:00Z"),
                Instant.parse("2026-09-24T15:00:00Z"),
                List.of()
        );
        when(ticketStatusService.transitionStatus(1L, TicketStatus.IN_PROGRESS)).thenReturn(response);

        mockMvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void updateTicketStatus_withInvalidTransition_returns409() throws Exception {
        when(ticketStatusService.transitionStatus(1L, TicketStatus.OPEN))
                .thenThrow(new InvalidStatusTransitionException(TicketStatus.CLOSED, TicketStatus.OPEN));

        mockMvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Invalid Status Transition"))
                .andExpect(jsonPath("$.message").value("Cannot transition from CLOSED to OPEN"))
                .andExpect(jsonPath("$.path").value("/api/tickets/1/status"));
    }

    @Test
    void updateTicketStatus_whenTicketMissing_returns404() throws Exception {
        when(ticketStatusService.transitionStatus(99L, TicketStatus.IN_PROGRESS))
                .thenThrow(new TicketNotFoundException(99L));

        mockMvc.perform(patch("/api/tickets/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found with id: 99"));
    }

    @Test
    void updateTicketStatus_withMissingStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
    }

    private TicketDetailResponse sampleDetailResponse(Long id) {
        return new TicketDetailResponse(
                id,
                "Cannot login",
                "User reports 500 error on login page.",
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                null,
                Instant.parse("2026-09-24T10:00:00Z"),
                Instant.parse("2026-09-24T14:30:00Z"),
                List.of()
        );
    }
}
