package com.queupid.pilago.web.resident;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.model.request.RequestStatus;
import com.queupid.pilago.service.RequestQueue;

import jakarta.servlet.http.HttpServletResponse;

/**
 * The resident's own ticket (R5–R9 / R15–R19). No login: the secret token in the link is the key.
 *
 * <pre>
 * GET /ticket                this phone's latest ticket (from a cookie), or "no ticket yet"
 * GET /ticket/{token}        the ticket page
 * GET /ticket/{token}/live   just the live part, polled by htmx every 5 seconds
 * </pre>
 */
@Controller
public class TicketPage {

    private final RequestQueue queue;

    public TicketPage(RequestQueue queue) {
        this.queue = queue;
    }

    @GetMapping("/ticket")
    public String myTicket(@CookieValue(name = ResidentPage.TICKET_COOKIE, required = false) String token) {
        Optional<Request> ticket = find(token);
        return ticket.map(r -> "redirect:/ticket/" + r.getToken()).orElse("resident/find-ticket");
    }

    @GetMapping("/ticket/{token}")
    public String ticket(@PathVariable String token, Model model, HttpServletResponse response) {
        Optional<Request> ticket = find(token);
        if (ticket.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            model.addAttribute("notFound", true);
            return "resident/find-ticket";
        }
        addLiveData(ticket.get(), model);
        return "resident/ticket";
    }

    @GetMapping("/ticket/{token}/live")
    public String live(@PathVariable String token, Model model, HttpServletResponse response) {
        Optional<Request> ticket = find(token);
        if (ticket.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "resident/find-ticket";
        }
        addLiveData(ticket.get(), model);
        return "resident/ticket :: live";
    }

    private void addLiveData(Request ticket, Model model) {
        model.addAttribute("ticket", ticket);
        model.addAttribute("nowServing", queue.getNowServing().map(Request::getTicketNumber).orElse("—"));
        model.addAttribute("ahead", queue.getPositionInLine(ticket.getTicketNumber()));
        model.addAttribute("waitMinutes", queue.getEstimatedWaitMinutes(ticket.getTicketNumber()));
        model.addAttribute("active", ticket.getStatus().isActive());
        model.addAttribute("called", ticket.getStatus() == RequestStatus.PROCESSING);
        // Status tracker: shown on the normal path only (not for On hold or Skipped)
        model.addAttribute("trackerSteps", TRACKER_STEPS);
        model.addAttribute("currentStep", TRACKER_STEPS.indexOf(ticket.getStatus()));
    }

    /** The normal path a request takes, in order, as the status tracker shows it. */
    static final List<RequestStatus> TRACKER_STEPS = List.of(
            RequestStatus.WAITING, RequestStatus.PROCESSING, RequestStatus.FOR_SIGNATURE,
            RequestStatus.READY, RequestStatus.RELEASED);

    /** A bad or old link is not an error page: it just finds nothing. */
    private Optional<Request> find(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return queue.findByToken(UUID.fromString(token));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
