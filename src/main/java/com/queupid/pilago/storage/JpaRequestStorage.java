package com.queupid.pilago.storage;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.queupid.pilago.model.request.Request;

/**
 * Keeps tickets in the database: H2 on a laptop, PostgreSQL (Neon) on Render. The resident is saved
 * together with the ticket ({@code cascade = ALL} on {@code Request.resident}).
 */
@Component
public class JpaRequestStorage implements RequestStorage {

    private final RequestRepository repository;

    public JpaRequestStorage(RequestRepository repository) {
        this.repository = repository;
    }

    /**
     * A new ticket is inserted and gets its id. A changed ticket is merged: the queue keeps using its own
     * copy, which already holds the same values.
     */
    @Override
    @Transactional
    public Request save(Request request) {
        return repository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Request> findByDate(LocalDate date) {
        return repository.findByTicketDateOrderByDailySequenceAsc(date);
    }

    @Override
    @Transactional(readOnly = true)
    public int lastSequence(LocalDate date) {
        return repository.findLastSequence(date);
    }
}
