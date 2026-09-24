package com.supportticket.service.status;

import com.supportticket.dto.response.CommentResponse;
import com.supportticket.dto.response.TicketDetailResponse;
import com.supportticket.entity.Ticket;
import com.supportticket.entity.TicketStatus;
import com.supportticket.exception.InvalidStatusTransitionException;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TicketStatusService {

    private static final Map<TicketStatus, Set<TicketStatus>> VALID_TRANSITIONS = Map.of(
            TicketStatus.OPEN, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            TicketStatus.IN_PROGRESS, Set.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED),
            TicketStatus.RESOLVED, Set.of(TicketStatus.CLOSED),
            TicketStatus.CLOSED, Set.of(),
            TicketStatus.CANCELLED, Set.of()
    );

    private final TicketRepository ticketRepository;

    public TicketStatusService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public TicketDetailResponse transitionStatus(Long ticketId, TicketStatus targetStatus) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        TicketStatus currentStatus = ticket.getStatus();
        if (!isValidTransition(currentStatus, targetStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, targetStatus);
        }

        ticket.setStatus(targetStatus);
        ticket.setUpdatedAt(Instant.now());
        return toDetailResponse(ticketRepository.save(ticket));
    }

    private boolean isValidTransition(TicketStatus current, TicketStatus target) {
        if (current == target) {
            return false;
        }
        return VALID_TRANSITIONS.getOrDefault(current, Set.of()).contains(target);
    }

    private TicketDetailResponse toDetailResponse(Ticket ticket) {
        List<CommentResponse> comments = ticket.getComments().stream()
                .map(comment -> new CommentResponse(
                        comment.getId(),
                        comment.getBody(),
                        comment.getAuthor(),
                        comment.getCreatedAt()))
                .toList();

        return new TicketDetailResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                comments
        );
    }
}
