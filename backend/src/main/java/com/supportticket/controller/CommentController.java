package com.supportticket.controller;

import com.supportticket.dto.request.CreateCommentRequest;
import com.supportticket.dto.response.CommentResponse;
import com.supportticket.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse created = commentService.addComment(ticketId, request);
        URI location = URI.create("/api/tickets/" + ticketId + "/comments/" + created.id());
        return ResponseEntity.created(location).body(created);
    }
}
