package com.queupid.pilago.storage;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.queupid.pilago.model.request.Request;

/** Spring Data access to the {@code document_request} table. Used only by {@link JpaRequestStorage}. */
public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByTicketDateOrderByDailySequenceAsc(LocalDate ticketDate);

    @Query("select coalesce(max(r.dailySequence), 0) from Request r where r.ticketDate = :date")
    int findLastSequence(@Param("date") LocalDate date);
}
