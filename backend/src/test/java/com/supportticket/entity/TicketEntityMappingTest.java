package com.supportticket.entity;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketEntityMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistAndLoadTicketWithComment() {
        Instant now = Instant.now();
        Ticket ticket = new Ticket(
                "Cannot login",
                "User reports 500 error on login page.",
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                "alice@example.com",
                now,
                now
        );
        Comment comment = new Comment(ticket, "Investigating logs.", "Bob", now);
        ticket.addComment(comment);

        entityManager.persist(ticket);
        entityManager.flush();
        entityManager.clear();

        Ticket loaded = entityManager.find(Ticket.class, ticket.getId());

        assertThat(loaded).isNotNull();
        assertThat(loaded.getTitle()).isEqualTo("Cannot login");
        assertThat(loaded.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(loaded.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(loaded.getComments()).hasSize(1);
        assertThat(loaded.getComments().getFirst().getBody()).isEqualTo("Investigating logs.");
    }
}
