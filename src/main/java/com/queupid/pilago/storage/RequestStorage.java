package com.queupid.pilago.storage;

import java.time.LocalDate;
import java.util.List;

import com.queupid.pilago.model.request.Request;

/**
 * Where tickets are kept so they survive a restart. The queue only talks to this interface, so the
 * same queue rules run on a real database ({@link JpaRequestStorage}) or on a plain list in the tests
 * ({@link InMemoryRequestStorage}). Two classes, one interface: polymorphism.
 */
public interface RequestStorage {

    /** Saves a new ticket or the latest change to an existing one, and returns it. */
    Request save(Request request);

    /** Every ticket issued on that day, in the order issued (by daily sequence). */
    List<Request> findByDate(LocalDate date);

    /** The highest daily sequence used on that day, or 0 if no ticket was issued yet. */
    int lastSequence(LocalDate date);
}
