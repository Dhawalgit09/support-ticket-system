package com.supportticket.service;

import com.supportticket.dto.request.CreateTicketRequest;
import com.supportticket.dto.request.UpdateTicketRequest;
import com.supportticket.dto.response.CommentResponse;
import com.supportticket.dto.response.TicketDetailResponse;
import com.supportticket.dto.response.TicketSummaryResponse;
import com.supportticket.entity.Ticket;
import com.supportticket.entity.TicketPriority;
import com.supportticket.entity.TicketStatus;
import com.supportticket.exception.FieldValidationException;
import com.supportticket.exception.StatusUpdateNotAllowedException;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public TicketDetailResponse create(CreateTicketRequest request) {
        Instant now = Instant.now();
        Ticket ticket = new Ticket(
                trim(request.title()),
                trim(request.description()),
                TicketStatus.OPEN,
                request.priority() != null ? request.priority() : TicketPriority.MEDIUM,
                normalizeOptionalText(request.assignee()),
                now,
                now
        );
        return toDetailResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getById(Long id) {
        return toDetailResponse(findTicketOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<TicketSummaryResponse> listTickets(String search, TicketStatus status) {
        String keyword = normalizeSearch(search);
        List<Ticket> tickets;

        if (keyword != null && status != null) {
            tickets = ticketRepository.searchByKeywordAndStatusOrderByUpdatedAtDesc(keyword, status);
        } else if (keyword != null) {
            tickets = ticketRepository.searchByKeywordOrderByUpdatedAtDesc(keyword);
        } else if (status != null) {
            tickets = ticketRepository.findByStatusOrderByUpdatedAtDesc(status);
        } else {
            tickets = ticketRepository.findAllByOrderByUpdatedAtDesc();
        }

        return tickets.stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional
    public TicketDetailResponse update(Long id, UpdateTicketRequest request) {
        if (request.status() != null) {
            throw new StatusUpdateNotAllowedException();
        }

        Ticket ticket = findTicketOrThrow(id);

        if (request.title() != null) {
            String trimmedTitle = trim(request.title());
            if (trimmedTitle.isEmpty()) {
                throw new FieldValidationException("title", "must not be blank", request.title());
            }
            ticket.setTitle(trimmedTitle);
        }
        if (request.description() != null) {
            String trimmedDescription = trim(request.description());
            if (trimmedDescription.isEmpty()) {
                throw new FieldValidationException("description", "must not be blank", request.description());
            }
            ticket.setDescription(trimmedDescription);
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.assigneeIncluded()) {
            ticket.setAssignee(normalizeOptionalText(request.assignee()));
        }

        ticket.setUpdatedAt(Instant.now());
        return toDetailResponse(ticketRepository.save(ticket));
    }

    private Ticket findTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    private TicketSummaryResponse toSummaryResponse(Ticket ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getUpdatedAt()
        );
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

    private static String normalizeSearch(String search) {
        return normalizeOptionalText(search);
    }
}
