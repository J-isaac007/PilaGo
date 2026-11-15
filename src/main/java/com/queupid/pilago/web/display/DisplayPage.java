package com.queupid.pilago.web.display;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * TV display board (Figma D1). Public, read-only, no login.
 * /display        -> full page
 * /display/board  -> just the board fragment, polled by htmx every 3 seconds
 */
@Controller
public class DisplayPage {

    /** What the template needs, nothing more (keeps the view free of domain classes). */
    public record Entry(String number, String priority) { }   // priority: SENIOR, PWD or REGULAR

    public record BoardView(String nowServing, int counter, List<String> next,
                            int waitingCount, List<Entry> waiting, List<String> ready) { }

    private static final int MAX_WAITING_SHOWN = 5;
    private static final int MAX_READY_SHOWN = 4;

    @Value("${pilago.public-url:pilago-sanisidro.onrender.com}")
    private String publicUrl;

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

    /**
     * Temporary sample data matching the Figma frame.
     * When Isaac's contract lands, build the BoardView from
     * RequestQueue.getNowServing(), getWaiting() and getReady()
     * (take the first MAX_*_SHOWN, and waiting.size() for waitingCount).
     */
    private BoardView load() {
        List<Entry> waiting = List.of(
                new Entry("S-004", "SENIOR"), new Entry("P-005", "PWD"),
                new Entry("S-008", "SENIOR"), new Entry("C-006", "REGULAR"),
                new Entry("C-007", "REGULAR"));
        List<String> ready = List.of("C-001", "S-002");
        return new BoardView("C-003", 1, List.of("S-004", "P-005", "S-008"),
                12, waiting.stream().limit(MAX_WAITING_SHOWN).toList(),
                ready.stream().limit(MAX_READY_SHOWN).toList());
    }
}
