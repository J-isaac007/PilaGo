package com.queupid.pilago.web.display;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.queupid.pilago.model.person.PriorityGroup;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.service.RequestQueue;

/**
 * TV display board (Figma D1). Public, read-only, no login.
 * <pre>
 * /display        full page
 * /display/board  just the board fragment, polled by htmx every 3 seconds
 * </pre>
 */
@Controller
public class DisplayPage {

    /** One line in the Waiting list. */
    public record Entry(String number, PriorityGroup priority) { }

    /** What the template needs, nothing more (keeps the view free of domain classes). */
    public record BoardView(String nowServing, int counter, List<String> next,
                            int waitingCount, List<Entry> waiting, List<String> ready) { }

    /** PilaGo has one counter. */
    private static final int COUNTER = 1;
    private static final int NEXT_SHOWN = 3;
    private static final int MAX_WAITING_SHOWN = 5;
    private static final int MAX_READY_SHOWN = 4;

    private final RequestQueue queue;

    @Value("${pilago.public-url:pilago-sanisidro.onrender.com}")
    private String publicUrl;

    public DisplayPage(RequestQueue queue) {
        this.queue = queue;
    }

    @GetMapping("/display")
    public String page(Model model) {
        model.addAttribute("board", load());
        model.addAttribute("publicUrl", publicUrl);
        return "display/board";
    }

    @GetMapping("/display/board")
    public String board(Model model) {
        model.addAttribute("board", load());
        return "display/board :: board";
    }

    private BoardView load() {
        List<Request> waiting = queue.getWaiting();
        List<Entry> shown = waiting.stream().limit(MAX_WAITING_SHOWN)
                .map(r -> new Entry(r.getTicketNumber(), r.getPriorityGroup())).toList();
        List<String> next = waiting.stream().limit(NEXT_SHOWN).map(Request::getTicketNumber).toList();
        List<String> ready = queue.getReady().stream().limit(MAX_READY_SHOWN).map(Request::getTicketNumber).toList();
        String nowServing = queue.getNowServing().map(Request::getTicketNumber).orElse(null);
        return new BoardView(nowServing, COUNTER, next, waiting.size(), shown, ready);
    }
}
