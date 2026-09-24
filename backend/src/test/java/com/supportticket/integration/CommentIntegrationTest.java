package com.supportticket.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportticket.repository.CommentRepository;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    @Test
    void addComment_returns201AndAppearsInTicketDetail() throws Exception {
        Long ticketId = createTicket();

        MvcResult createResult = mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Investigating logs.",
                                  "author": "Bob"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.body").value("Investigating logs."))
                .andExpect(jsonPath("$.author").value("Bob"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        Long commentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        assertThat(createResult.getResponse().getHeader("Location"))
                .isEqualTo("/api/tickets/" + ticketId + "/comments/" + commentId);

        mockMvc.perform(get("/api/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].id").value(commentId))
                .andExpect(jsonPath("$.comments[0].body").value("Investigating logs."))
                .andExpect(jsonPath("$.comments[0].author").value("Bob"));
    }

    @Test
    void addComment_updatesTicketUpdatedAt() throws Exception {
        Long ticketId = createTicket();
        Instant previousUpdatedAt = Instant.parse("2026-01-01T10:00:00Z");

        ticketRepository.findById(ticketId).ifPresent(ticket -> {
            ticket.setUpdatedAt(previousUpdatedAt);
            ticketRepository.save(ticket);
        });

        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Follow-up note."
                                }
                                """))
                .andExpect(status().isCreated());

        assertThat(ticketRepository.findById(ticketId))
                .isPresent()
                .get()
                .extracting(ticket -> ticket.getUpdatedAt())
                .isNotEqualTo(previousUpdatedAt)
                .satisfies(updatedAt -> assertThat((Instant) updatedAt).isAfter(previousUpdatedAt));
    }

    @Test
    void addComment_whenTicketMissing_returns404() throws Exception {
        mockMvc.perform(post("/api/tickets/99/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Investigating logs."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ticket not found with id: 99"));
    }

    @Test
    void addComment_withEmptyBody_returns400() throws Exception {
        Long ticketId = createTicket();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("body"));
    }

    private Long createTicket() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Comment integration test",
                                  "description": "Ticket for comment tests"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }
}
