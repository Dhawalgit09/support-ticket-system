package com.supportticket.service;

import com.supportticket.dto.request.CreateCommentRequest;
import com.supportticket.dto.response.CommentResponse;
import com.supportticket.entity.Ticket;
import com.supportticket.entity.TicketPriority;
import com.supportticket.entity.TicketStatus;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.entity.Comment;
import com.supportticket.repository.CommentRepository;
import com.supportticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void addComment_toExistingTicket_savesCommentAndBumpsTicketUpdatedAt() {
        Instant previousUpdatedAt = Instant.parse("2026-01-02T10:00:00Z");
        Ticket ticket = new Ticket(
                "Title",
                "Description",
                TicketStatus.OPEN,
                TicketPriority.MEDIUM,
                null,
                Instant.parse("2026-01-01T10:00:00Z"),
                previousUpdatedAt
        );
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(commentRepository.saveAndFlush(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCommentRequest request = new CreateCommentRequest("Investigating logs.", "Bob");
        CommentResponse response = commentService.addComment(1L, request);

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(commentRepository).saveAndFlush(any(Comment.class));
        verify(ticketRepository).save(captor.capture());
        Ticket savedTicket = captor.getValue();

        assertThat(savedTicket.getComments()).hasSize(1);
        assertThat(savedTicket.getComments().getFirst().getBody()).isEqualTo("Investigating logs.");
        assertThat(savedTicket.getComments().getFirst().getAuthor()).isEqualTo("Bob");
        assertThat(savedTicket.getUpdatedAt()).isAfter(previousUpdatedAt);
        assertThat(response.body()).isEqualTo("Investigating logs.");
        assertThat(response.author()).isEqualTo("Bob");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void addComment_whenTicketMissing_throwsTicketNotFoundException() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        CreateCommentRequest request = new CreateCommentRequest("Investigating logs.", null);

        assertThatThrownBy(() -> commentService.addComment(99L, request))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("Ticket not found with id: 99");

        verify(ticketRepository, never()).save(any());
    }
}
