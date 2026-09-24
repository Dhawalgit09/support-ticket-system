package com.supportticket.repository;

import com.supportticket.entity.Ticket;
import com.supportticket.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findAllByOrderByUpdatedAtDesc();

    List<Ticket> findByStatusOrderByUpdatedAtDesc(TicketStatus status);

    @Query("""
            SELECT t FROM Ticket t
            WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY t.updatedAt DESC
            """)
    List<Ticket> searchByKeywordOrderByUpdatedAtDesc(@Param("search") String search);

    @Query("""
            SELECT t FROM Ticket t
            WHERE (LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))
              AND t.status = :status
            ORDER BY t.updatedAt DESC
            """)
    List<Ticket> searchByKeywordAndStatusOrderByUpdatedAtDesc(
            @Param("search") String search,
            @Param("status") TicketStatus status);
}
