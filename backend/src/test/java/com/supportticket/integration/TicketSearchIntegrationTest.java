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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketSearchIntegrationTest {

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
    void searchMatchesTitle_returnsMatchingTicket() throws Exception {
        createTicket("Cannot login", "Generic issue");
        createTicket("Billing question", "Invoice mismatch");

        mockMvc.perform(get("/api/tickets").param("search", "login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Cannot login"));
    }

    @Test
    void searchMatchesDescription_returnsMatchingTicket() throws Exception {
        createTicket("Account issue", "User reports 500 error on login page");
        createTicket("Other issue", "No relevant keywords here");

        mockMvc.perform(get("/api/tickets").param("search", "login page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Account issue"));
    }

    @Test
    void searchIsCaseInsensitive_returnsMatchingTicket() throws Exception {
        createTicket("Password Reset", "Forgot password flow broken");

        mockMvc.perform(get("/api/tickets").param("search", "password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Password Reset"));
    }

    @Test
    void filterByStatus_returnsOnlyMatchingTickets() throws Exception {
        Long openTicketId = createTicket("Open ticket", "Still open");
        createTicket("Another open ticket", "Also open");
        Long inProgressTicketId = createTicket("In progress ticket", "Being worked");

        transitionStatus(inProgressTicketId, TicketStatus.IN_PROGRESS);

        mockMvc.perform(get("/api/tickets").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == " + openTicketId + ")].status").value("OPEN"))
                .andExpect(jsonPath("$[?(@.id == " + inProgressTicketId + ")]").doesNotExist());
    }

    @Test
    void combinedSearchAndStatus_returnsTicketsMatchingBoth() throws Exception {
        Long openLoginId = createTicket("Login failure", "Cannot sign in");
        Long cancelledLoginId = createTicket("Login timeout", "Session expires");
        createTicket("Billing issue", "Payment failed");

        transitionStatus(cancelledLoginId, TicketStatus.CANCELLED);

        mockMvc.perform(get("/api/tickets")
                        .param("search", "login")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(openLoginId))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void searchWithNoMatches_returnsEmptyArray() throws Exception {
        createTicket("Existing ticket", "Nothing special");

        mockMvc.perform(get("/api/tickets").param("search", "nonexistent-keyword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private Long createTicket(String title, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "%s"
                                }
                                """.formatted(title, description)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private void transitionStatus(Long ticketId, TicketStatus status) throws Exception {
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }
}
