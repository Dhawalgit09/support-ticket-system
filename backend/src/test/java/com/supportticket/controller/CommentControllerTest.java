package com.supportticket.controller;

import com.supportticket.dto.request.CreateCommentRequest;
import com.supportticket.dto.response.CommentResponse;
import com.supportticket.exception.GlobalExceptionHandler;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import(GlobalExceptionHandler.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @Test
    void addComment_withValidBody_returns201AndLocationHeader() throws Exception {
        CommentResponse response = new CommentResponse(
                10L,
                "Investigating logs.",
                "Bob",
                Instant.parse("2026-09-24T12:00:00Z")
        );
        when(commentService.addComment(eq(1L), any(CreateCommentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tickets/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Investigating logs.",
                                  "author": "Bob"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tickets/1/comments/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.body").value("Investigating logs."))
                .andExpect(jsonPath("$.author").value("Bob"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void addComment_withEmptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/tickets/1/comments")
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

    @Test
    void addComment_whenTicketMissing_returns404() throws Exception {
        when(commentService.addComment(eq(99L), any(CreateCommentRequest.class)))
                .thenThrow(new TicketNotFoundException(99L));

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
}
