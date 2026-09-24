package com.supportticket.service.status;

import com.supportticket.dto.response.TicketDetailResponse;
import com.supportticket.entity.Ticket;
import com.supportticket.entity.TicketPriority;
import com.supportticket.entity.TicketStatus;
import com.supportticket.exception.InvalidStatusTransitionException;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
class TicketStatusServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketStatusService ticketStatusService;

    @Test
    void transitionStatus_openToInProgress_succeeds() {
        assertValidTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
    }

    @Test
    void transitionStatus_openToCancelled_succeeds() {
        assertValidTransition(TicketStatus.OPEN, TicketStatus.CANCELLED);
    }

    @Test
    void transitionStatus_inProgressToResolved_succeeds() {
        assertValidTransition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED);
    }

    @Test
    void transitionStatus_inProgressToCancelled_succeeds() {
        assertValidTransition(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED);
    }

    @Test
    void transitionStatus_resolvedToClosed_succeeds() {
        assertValidTransition(TicketStatus.RESOLVED, TicketStatus.CLOSED);
    }

    @Test
    void transitionStatus_fromClosedToOpen_throwsAndDoesNotSave() {
        assertInvalidTransition(TicketStatus.CLOSED, TicketStatus.OPEN);
    }

    @Test
    void transitionStatus_fromResolvedToOpen_throwsAndDoesNotSave() {
        assertInvalidTransition(TicketStatus.RESOLVED, TicketStatus.OPEN);
    }

    @Test
    void transitionStatus_fromCancelledToOpen_throwsAndDoesNotSave() {
        assertInvalidTransition(TicketStatus.CANCELLED, TicketStatus.OPEN);
    }

    @Test
    void transitionStatus_fromResolvedToInProgress_throwsAndDoesNotSave() {
        assertInvalidTransition(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS);
    }

    @Test
    void transitionStatus_fromOpenToResolved_throwsAndDoesNotSave() {
        assertInvalidTransition(TicketStatus.OPEN, TicketStatus.RESOLVED);
    }

    @Test
    void transitionStatus_sameState_throwsAndDoesNotSave() {
        assertInvalidTransition(TicketStatus.OPEN, TicketStatus.OPEN);
    }

    @Test
    void transitionStatus_fromClosedToCancelled_throwsAndDoesNotSave() {
        assertInvalidTransition(TicketStatus.CLOSED, TicketStatus.CANCELLED);
    }

    @Test
    void transitionStatus_fromInProgressToOpen_throwsAndDoesNotSave() {
        assertInvalidTransition(TicketStatus.IN_PROGRESS, TicketStatus.OPEN);
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"CLOSED", "CANCELLED"})
    void transitionStatus_fromTerminalState_throwsAndDoesNotSave(TicketStatus terminalStatus) {
        assertInvalidTransition(terminalStatus, TicketStatus.IN_PROGRESS);
    }

    @Test
    void transitionStatus_whenTicketMissing_throwsTicketNotFoundException() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketStatusService.transitionStatus(99L, TicketStatus.IN_PROGRESS))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("Ticket not found with id: 99");

        verify(ticketRepository, never()).save(any());
    }

    private void assertValidTransition(TicketStatus from, TicketStatus to) {
        Ticket ticket = ticketWithStatus(from);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketDetailResponse response = ticketStatusService.transitionStatus(1L, to);

        assertThat(ticket.getStatus()).isEqualTo(to);
        assertThat(response.status()).isEqualTo(to);
        assertThat(ticket.getUpdatedAt()).isAfter(Instant.parse("2026-01-02T10:00:00Z"));
        verify(ticketRepository).save(ticket);
    }

    private void assertInvalidTransition(TicketStatus from, TicketStatus to) {
        Ticket ticket = ticketWithStatus(from);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketStatusService.transitionStatus(1L, to))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Cannot transition from " + from + " to " + to);

        assertThat(ticket.getStatus()).isEqualTo(from);
        verify(ticketRepository, never()).save(any());
    }

    private Ticket ticketWithStatus(TicketStatus status) {
        return new Ticket(
                "Title",
                "Description",
                status,
                TicketPriority.MEDIUM,
                null,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-02T10:00:00Z")
        );
    }
}
