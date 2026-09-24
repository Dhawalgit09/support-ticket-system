package com.supportticket.service;

import com.supportticket.dto.request.CreateCommentRequest;
import com.supportticket.dto.response.CommentResponse;
import com.supportticket.entity.Comment;
import com.supportticket.entity.Ticket;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.repository.CommentRepository;
import com.supportticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CommentService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;

    public CommentService(TicketRepository ticketRepository, CommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, CreateCommentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        Instant now = Instant.now();
        Comment comment = new Comment(
                ticket,
                trim(request.body()),
                normalizeOptionalText(request.author()),
                now
        );
        ticket.addComment(comment);
        ticket.setUpdatedAt(now);
        Comment saved = commentRepository.saveAndFlush(comment);
        ticketRepository.save(ticket);

        return toResponse(saved);
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getBody(),
                comment.getAuthor(),
                comment.getCreatedAt()
        );
    }

    private static String trim(String value) {
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
