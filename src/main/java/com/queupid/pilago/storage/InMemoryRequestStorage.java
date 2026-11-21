package com.queupid.pilago.storage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.queupid.pilago.model.request.Request;

/**
 * Keeps tickets in a plain {@link ArrayList}. Not a Spring bean: unit tests create it with {@code new}
 * so the queue rules can be tested without a database, including "restart" (a new queue on the same
 * storage).
 */
public class InMemoryRequestStorage implements RequestStorage {

    private final List<Request> saved = new ArrayList<>();

    @Override
    public synchronized Request save(Request request) {
        if (!saved.contains(request)) {
            saved.add(request);
        }
        return request;
    }

    @Override
    public synchronized List<Request> findByDate(LocalDate date) {
        List<Request> result = new ArrayList<>();
        for (Request r : saved) {
            if (r.getTicketDate().equals(date)) {
                result.add(r);
            }
        }
        result.sort(Comparator.comparingInt(Request::getDailySequence));
        return result;
    }

    @Override
    public synchronized int lastSequence(LocalDate date) {
        int last = 0;
        for (Request r : saved) {
            if (r.getTicketDate().equals(date)) {
                last = Math.max(last, r.getDailySequence());
            }
        }
        return last;
    }
}
