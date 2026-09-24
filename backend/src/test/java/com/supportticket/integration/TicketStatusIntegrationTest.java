package com.supportticket.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportticket.entity.TicketStatus;
import com.supportticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    void smIt01_openToInProgress_updatesDbStatus() throws Exception {
        Long ticketId = createTicket();

        transitionStatusExpectOk(ticketId, TicketStatus.IN_PROGRESS);

        assertDbStatus(ticketId, TicketStatus.IN_PROGRESS);
    }

    @Test
    void smIt02_openToCancelled_updatesDbStatus() throws Exception {
        Long ticketId = createTicket();

        transitionStatusExpectOk(ticketId, TicketStatus.CANCELLED);

        assertDbStatus(ticketId, TicketStatus.CANCELLED);
    }

    @Test
    void smIt03_inProgressToResolved_updatesDbStatus() throws Exception {
        Long ticketId = createTicket();
        transitionStatusExpectOk(ticketId, TicketStatus.IN_PROGRESS);

        transitionStatusExpectOk(ticketId, TicketStatus.RESOLVED);

        assertDbStatus(ticketId, TicketStatus.RESOLVED);
    }

    @Test
    void smIt04_inProgressToCancelled_updatesDbStatus() throws Exception {
        Long ticketId = createTicket();
        transitionStatusExpectOk(ticketId, TicketStatus.IN_PROGRESS);

        transitionStatusExpectOk(ticketId, TicketStatus.CANCELLED);

        assertDbStatus(ticketId, TicketStatus.CANCELLED);
    }

    @Test
    void smIt05_resolvedToClosed_updatesDbStatus() throws Exception {
        Long ticketId = createTicket();
        moveToResolved(ticketId);

        transitionStatusExpectOk(ticketId, TicketStatus.CLOSED);

        assertDbStatus(ticketId, TicketStatus.CLOSED);
    }

    @Test
    void smIt06_fullChainOnSingleTicket_endsClosed() throws Exception {
        Long ticketId = createTicket();

        transitionStatusExpectOk(ticketId, TicketStatus.IN_PROGRESS);
        transitionStatusExpectOk(ticketId, TicketStatus.RESOLVED);
        transitionStatusExpectOk(ticketId, TicketStatus.CLOSED);

        assertDbStatus(ticketId, TicketStatus.CLOSED);
    }

    @Test
    void smIt07_closedToOpen_returns409AndUnchangedStatus() throws Exception {
        Long ticketId = createTicket();
        moveToClosed(ticketId);

        transitionStatusExpectConflict(ticketId, TicketStatus.CLOSED, TicketStatus.OPEN);

        assertDbStatus(ticketId, TicketStatus.CLOSED);
    }

    @Test
    void smIt08_resolvedToOpen_returns409AndUnchangedStatus() throws Exception {
        Long ticketId = createTicket();
        moveToResolved(ticketId);

        transitionStatusExpectConflict(ticketId, TicketStatus.RESOLVED, TicketStatus.OPEN);

        assertDbStatus(ticketId, TicketStatus.RESOLVED);
    }

    @Test
    void smIt09_cancelledToOpen_returns409AndUnchangedStatus() throws Exception {
        Long ticketId = createTicket();
        transitionStatusExpectOk(ticketId, TicketStatus.CANCELLED);

        transitionStatusExpectConflict(ticketId, TicketStatus.CANCELLED, TicketStatus.OPEN);

        assertDbStatus(ticketId, TicketStatus.CANCELLED);
    }

    @Test
    void smIt10_resolvedToInProgress_returns409AndUnchangedStatus() throws Exception {
        Long ticketId = createTicket();
        moveToResolved(ticketId);

        transitionStatusExpectConflict(ticketId, TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS);

        assertDbStatus(ticketId, TicketStatus.RESOLVED);
    }

    @Test
    void smIt11_openToResolved_returns409AndUnchangedStatus() throws Exception {
        Long ticketId = createTicket();

        transitionStatusExpectConflict(ticketId, TicketStatus.OPEN, TicketStatus.RESOLVED);

        assertDbStatus(ticketId, TicketStatus.OPEN);
    }

    @Test
    void smIt12_openToOpen_returns409AndUnchangedStatus() throws Exception {
        Long ticketId = createTicket();

        transitionStatusExpectConflict(ticketId, TicketStatus.OPEN, TicketStatus.OPEN);

        assertDbStatus(ticketId, TicketStatus.OPEN);
    }

    @Test
    void smIt13_closedToCancelled_returns409AndUnchangedStatus() throws Exception {
        Long ticketId = createTicket();
        moveToClosed(ticketId);

        transitionStatusExpectConflict(ticketId, TicketStatus.CLOSED, TicketStatus.CANCELLED);

        assertDbStatus(ticketId, TicketStatus.CLOSED);
    }

    @Test
    void smIt14_statusOnFieldPatch_returns400() throws Exception {
        Long ticketId = createTicket();

        mockMvc.perform(patch("/api/tickets/" + ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Status cannot be updated via this endpoint. Use PATCH /api/tickets/{id}/status"));

        assertDbStatus(ticketId, TicketStatus.OPEN);
    }

    @Test
    void smIt15_createTicket_hasOpenStatusInResponseAndDb() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "New ticket",
                                  "description": "Created for SM-IT-15"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Long ticketId = body.get("id").asLong();

        assertDbStatus(ticketId, TicketStatus.OPEN);
    }

    private Long createTicket() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "State machine test",
                                  "description": "Ticket for integration test"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void moveToResolved(Long ticketId) throws Exception {
        transitionStatusExpectOk(ticketId, TicketStatus.IN_PROGRESS);
        transitionStatusExpectOk(ticketId, TicketStatus.RESOLVED);
    }

    private void moveToClosed(Long ticketId) throws Exception {
        moveToResolved(ticketId);
        transitionStatusExpectOk(ticketId, TicketStatus.CLOSED);
    }

    private void transitionStatusExpectOk(Long ticketId, TicketStatus status) throws Exception {
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status.name()));
    }

    private void transitionStatusExpectConflict(
            Long ticketId,
            TicketStatus current,
            TicketStatus requested) throws Exception {
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + requested + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Invalid Status Transition"))
                .andExpect(jsonPath("$.message").value(
                        "Cannot transition from " + current + " to " + requested));
    }

    private void assertDbStatus(Long ticketId, TicketStatus expected) {
        assertThat(ticketRepository.findById(ticketId))
                .isPresent()
                .get()
                .extracting(ticket -> ticket.getStatus())
                .isEqualTo(expected);
    }
}
