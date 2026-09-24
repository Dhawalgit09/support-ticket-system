package com.supportticket.service;

import com.supportticket.dto.request.CreateTicketRequest;
import com.supportticket.dto.request.UpdateTicketRequest;
import com.supportticket.dto.response.TicketSummaryResponse;
import com.supportticket.entity.Ticket;
import com.supportticket.entity.TicketPriority;
import com.supportticket.entity.TicketStatus;
import com.supportticket.exception.FieldValidationException;
import com.supportticket.exception.StatusUpdateNotAllowedException;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void createTicket_withValidInput_defaultsOpenAndMedium() {
        CreateTicketRequest request = new CreateTicketRequest("Cannot login", "User reports error", null, null);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.create(request);

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        Ticket saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(saved.getPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(saved.getTitle()).isEqualTo("Cannot login");
        assertThat(saved.getDescription()).isEqualTo("User reports error");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    @Test
    void updateTicket_withPartialFields_updatesOnlyProvidedFieldsAndUpdatedAt() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant previousUpdatedAt = Instant.parse("2026-01-02T10:00:00Z");
        Ticket ticket = new Ticket(
                "Old title",
                "Old description",
                TicketStatus.OPEN,
                TicketPriority.MEDIUM,
                "alice@example.com",
                createdAt,
                previousUpdatedAt
        );

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTicketRequest request = new UpdateTicketRequest("New title", null, TicketPriority.HIGH, null, null);
        ticketService.update(1L, request);

        assertThat(ticket.getTitle()).isEqualTo("New title");
        assertThat(ticket.getDescription()).isEqualTo("Old description");
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(ticket.getAssignee()).isEqualTo("alice@example.com");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getUpdatedAt()).isAfter(previousUpdatedAt);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void updateTicket_withExplicitNullAssignee_clearsAssignee() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant previousUpdatedAt = Instant.parse("2026-01-02T10:00:00Z");
        Ticket ticket = new Ticket(
                "Title",
                "Description",
                TicketStatus.OPEN,
                TicketPriority.MEDIUM,
                "alice@example.com",
                createdAt,
                previousUpdatedAt
        );

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setAssignee(null);

        ticketService.update(1L, request);

        assertThat(ticket.getAssignee()).isNull();
        verify(ticketRepository).save(ticket);
    }

    @Test
    void updateTicket_withWhitespaceOnlyTitle_throwsFieldValidationException() {
        Ticket ticket = sampleTicket();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        UpdateTicketRequest request = new UpdateTicketRequest("   ", null, null, null, null);

        assertThatThrownBy(() -> ticketService.update(1L, request))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(ex -> {
                    FieldValidationException fieldError = (FieldValidationException) ex;
                    assertThat(fieldError.field()).isEqualTo("title");
                    assertThat(fieldError.getMessage()).isEqualTo("must not be blank");
                });

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void updateTicket_withWhitespaceOnlyDescription_throwsFieldValidationException() {
        Ticket ticket = sampleTicket();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        UpdateTicketRequest request = new UpdateTicketRequest(null, "   ", null, null, null);

        assertThatThrownBy(() -> ticketService.update(1L, request))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(ex -> {
                    FieldValidationException fieldError = (FieldValidationException) ex;
                    assertThat(fieldError.field()).isEqualTo("description");
                });

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void updateTicket_withStatusInRequest_rejectsBeforePersistence() {
        UpdateTicketRequest request = new UpdateTicketRequest(null, null, null, null, TicketStatus.IN_PROGRESS);

        assertThatThrownBy(() -> ticketService.update(1L, request))
                .isInstanceOf(StatusUpdateNotAllowedException.class);

        verify(ticketRepository, never()).findById(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void getById_whenTicketMissing_throwsTicketNotFoundException() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getById(99L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("Ticket not found with id: 99");
    }

    @Test
    void listTickets_withNoParams_delegatesToFindAll() {
        Ticket ticket = sampleTicket();
        when(ticketRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(ticket));

        List<TicketSummaryResponse> responses = ticketService.listTickets(null, null);

        verify(ticketRepository).findAllByOrderByUpdatedAtDesc();
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().title()).isEqualTo("Cannot login");
        assertThat(responses.getFirst().status()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void listTickets_withStatusOnly_delegatesToFindByStatus() {
        when(ticketRepository.findByStatusOrderByUpdatedAtDesc(TicketStatus.OPEN)).thenReturn(List.of());

        ticketService.listTickets(null, TicketStatus.OPEN);

        verify(ticketRepository).findByStatusOrderByUpdatedAtDesc(TicketStatus.OPEN);
    }

    @Test
    void listTickets_withSearchOnly_delegatesToSearchQuery() {
        when(ticketRepository.searchByKeywordOrderByUpdatedAtDesc("login")).thenReturn(List.of());

        ticketService.listTickets("login", null);

        verify(ticketRepository).searchByKeywordOrderByUpdatedAtDesc("login");
    }

    @Test
    void listTickets_withSearchAndStatus_delegatesToCombinedQuery() {
        when(ticketRepository.searchByKeywordAndStatusOrderByUpdatedAtDesc("login", TicketStatus.OPEN))
                .thenReturn(List.of());

        ticketService.listTickets("login", TicketStatus.OPEN);

        verify(ticketRepository).searchByKeywordAndStatusOrderByUpdatedAtDesc("login", TicketStatus.OPEN);
    }

    @Test
    void listTickets_withBlankSearch_ignoresSearchParam() {
        when(ticketRepository.findByStatusOrderByUpdatedAtDesc(TicketStatus.OPEN)).thenReturn(List.of());

        ticketService.listTickets("   ", TicketStatus.OPEN);

        verify(ticketRepository).findByStatusOrderByUpdatedAtDesc(TicketStatus.OPEN);
        verify(ticketRepository, never()).searchByKeywordAndStatusOrderByUpdatedAtDesc(any(), eq(TicketStatus.OPEN));
    }

    private Ticket sampleTicket() {
        Instant now = Instant.parse("2026-01-02T10:00:00Z");
        return new Ticket(
                "Cannot login",
                "User reports error",
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                null,
                Instant.parse("2026-01-01T10:00:00Z"),
                now
        );
    }
}
