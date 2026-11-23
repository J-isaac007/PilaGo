package com.queupid.pilago.web.staff;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.queupid.pilago.exception.NotAResidentException;
import com.queupid.pilago.model.person.StaffAction;
import com.queupid.pilago.model.person.StaffMember;
import com.queupid.pilago.model.request.HoldReason;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.model.request.RequestStatus;
import com.queupid.pilago.service.RequestQueue;
import com.queupid.pilago.web.Bilingual;

/**
 * The secretary's pages.
 *
 * <pre>
 * GET  /staff                         S2 / S7 / S9  dashboard: now serving, tabs, queue, daily summary
 * POST /staff/call-next                             WAITING → PROCESSING
 * POST /staff/skip/{ticket}                         → NO_SHOW (after a confirm dialog)
 * GET  /staff/request/{ticket}        S3 / S10      process request
 * POST /staff/request/{ticket}/pay                  record payment
 * POST /staff/request/{ticket}/forward              PROCESSING → FOR_SIGNATURE
 * POST /staff/request/{ticket}/hold                 → ON_HOLD with a reason
 * POST /staff/request/{ticket}/verify               ON_HOLD → WAITING (keeps its place)
 * POST /staff/release/{ticket}        S6 / S13      READY → RELEASED (after a confirm dialog)
 * </pre>
 *
 * Every action first checks {@code StaffMember.canPerform(...)} through {@link CurrentStaff}. Queue errors
 * ({@code InvalidStatusTransitionException}, {@code QueueBusyException}…) are turned into a red toast by
 * {@code GlobalExceptionHandler}.
 */
@Controller
@RequestMapping("/staff")
public class StaffPage {

    /** Dashboard tabs. */
    public enum Tab {
        WAITING("waiting", "status.waiting"),
        ON_HOLD("on-hold", "status.on-hold"),
        READY("ready", "status.ready"),
        SKIPPED("skipped", "status.skipped");

        private final String slug;
        private final String messageKey;

        Tab(String slug, String messageKey) {
            this.slug = slug;
            this.messageKey = messageKey;
        }

        public String getSlug() {
            return slug;
        }

        public String getMessageKey() {
            return messageKey;
        }

        static Tab from(String slug) {
            for (Tab tab : values()) {
                if (tab.slug.equals(slug)) {
                    return tab;
                }
            }
            return WAITING;
        }
    }

    /** One queue row, with how many minutes the ticket has waited. */
    public record Row(Request request, long minutes) { }

    /** One tab link with its count. */
    public record TabLink(Tab tab, int count, boolean active) { }

    /** The numbers in the Daily summary column. */
    public record Summary(int total, int released, int onHold, String fees, long paidCount,
                          int forSignature, long oldestSignatureMinutes, long averageWait, long priorityAverageWait) { }

    private final RequestQueue queue;
    private final CurrentStaff current;
    private final Bilingual bi;
    private final Clock clock;

    public StaffPage(RequestQueue queue, CurrentStaff current, Bilingual bi, Clock clock) {
        this.queue = queue;
        this.current = current;
        this.bi = bi;
        this.clock = clock;
    }

    @ModelAttribute("me")
    public StaffMember me(Principal principal) {
        return current.get(principal);
    }

    // ── S2 · Dashboard ───────────────────────────────────────────────────

    @GetMapping
    public String dashboard(@RequestParam(defaultValue = "waiting") String tab,
            @RequestParam(defaultValue = "") String q,
            @RequestHeader(value = "HX-Request", required = false) String htmx,
            Principal principal, Model model) {
        current.require(principal, StaffAction.VIEW_DAILY_SUMMARY);
        Tab active = Tab.from(tab);
        List<Request> waiting = queue.getWaiting();
        List<Request> onHold = queue.getOnHold();
        List<Request> ready = queue.getReady();
        List<Request> skipped = queue.getAllToday().stream().filter(r -> r.getStatus() == RequestStatus.NO_SHOW).toList();

        List<Request> shown = switch (active) {
            case WAITING -> waiting;
            case ON_HOLD -> onHold;
            case READY -> ready;
            case SKIPPED -> skipped;
        };
        List<Row> rows = new ArrayList<>();
        for (Request r : shown) {
            if (matches(r, q)) {
                rows.add(new Row(r, minutesSince(r.getCreatedAt())));
            }
        }

        model.addAttribute("tab", active);
        model.addAttribute("q", q);
        model.addAttribute("tabs", List.of(
                new TabLink(Tab.WAITING, waiting.size(), active == Tab.WAITING),
                new TabLink(Tab.ON_HOLD, onHold.size(), active == Tab.ON_HOLD),
                new TabLink(Tab.READY, ready.size(), active == Tab.READY),
                new TabLink(Tab.SKIPPED, skipped.size(), active == Tab.SKIPPED)));
        model.addAttribute("rows", rows);
        model.addAttribute("shownTotal", shown.size());
        model.addAttribute("nowServing", queue.getNowServing().orElse(null));
        model.addAttribute("servingMinutes", queue.getNowServing().map(r -> minutesSince(r.getCalledAt())).orElse(0L));
        model.addAttribute("summary", summary());
        model.addAttribute("today", LocalDate.now(clock));
        return htmx != null ? "staff/dashboard :: board" : "staff/dashboard";
    }

    /** P4 · Download today's tickets as a CSV file (secretary only, like everything under /staff). */
    @GetMapping("/report.csv")
    public ResponseEntity<byte[]> report(Principal principal) {
        current.require(principal, StaffAction.VIEW_DAILY_SUMMARY);
        String csv = new ReportCsv(key -> bi.en(key)).render(queue.getAllToday());
        String filename = "pilago-report-" + LocalDate.now(clock) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/call-next")
    public String callNext(Principal principal, RedirectAttributes flash) {
        current.require(principal, StaffAction.CALL_NEXT);
        queue.callNext().ifPresentOrElse(
                r -> toast(flash, "staff.toast.called", r.getTicketNumber()),
                () -> toast(flash, "staff.toast.nobody"));
        return "redirect:/staff";
    }

    @PostMapping("/skip/{ticket}")
    public String skip(@PathVariable String ticket, Principal principal, RedirectAttributes flash) {
        current.require(principal, StaffAction.SKIP);
        queue.skip(ticket);
        toast(flash, "staff.toast.skipped", ticket);
        return "redirect:/staff";
    }

    // ── S3 · Process request ─────────────────────────────────────────────

    @GetMapping("/request/{ticket}")
    public String request(@PathVariable String ticket, Model model) {
        Request request = queue.findByTicket(ticket)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No ticket " + ticket));
        HoldReason residencyProblem = null;
        try {
            request.getResident().checkResidency();
        } catch (NotAResidentException e) {
            residencyProblem = e.getReason();
        }
        model.addAttribute("ticket", request);
        model.addAttribute("residencyProblem", residencyProblem);
        model.addAttribute("waited", request.getCalledAt() == null
                ? minutesSince(request.getCreatedAt())
                : Duration.between(request.getCreatedAt(), request.getCalledAt()).toMinutes());
        model.addAttribute("canForward", request.getStatus() == RequestStatus.PROCESSING
                && (request.isPaid() || request.isFree()));
        model.addAttribute("holdReasons", List.of(HoldReason.MISSING_REQUIREMENT, HoldReason.SHORT_RESIDENCY,
                HoldReason.PENDING_RECORD, HoldReason.OTHER));
        return "staff/request";
    }

    @PostMapping("/request/{ticket}/pay")
    public String pay(@PathVariable String ticket, Principal principal, RedirectAttributes flash) {
        current.require(principal, StaffAction.RECORD_PAYMENT);
        queue.markPaid(ticket);
        toast(flash, "staff.toast.paid", ticket);
        return "redirect:/staff/request/" + ticket;
    }

    @PostMapping("/request/{ticket}/forward")
    public String forward(@PathVariable String ticket, Principal principal, RedirectAttributes flash) {
        current.require(principal, StaffAction.FORWARD_TO_CAPTAIN);
        queue.forwardToCaptain(ticket);
        toast(flash, "staff.toast.forwarded", ticket);
        return "redirect:/staff";
    }

    @PostMapping("/request/{ticket}/hold")
    public String hold(@PathVariable String ticket, @RequestParam HoldReason reason,
            @RequestParam(defaultValue = "") String note, Principal principal, RedirectAttributes flash) {
        current.require(principal, StaffAction.PUT_ON_HOLD);
        queue.putOnHold(ticket, reason, note.length() > 200 ? note.substring(0, 200) : note);
        toast(flash, "staff.toast.held", ticket);
        return "redirect:/staff?tab=on-hold";
    }

    @PostMapping("/request/{ticket}/verify")
    public String verify(@PathVariable String ticket, Principal principal, RedirectAttributes flash) {
        current.require(principal, StaffAction.VERIFY);
        queue.verify(ticket);
        toast(flash, "staff.toast.verified", ticket);
        return "redirect:/staff?tab=on-hold";
    }

    // ── S6 · Release ─────────────────────────────────────────────────────

    @PostMapping("/release/{ticket}")
    public String release(@PathVariable String ticket, Principal principal, RedirectAttributes flash) {
        current.require(principal, StaffAction.RELEASE);
        queue.release(ticket);
        toast(flash, "staff.toast.released", ticket);
        return "redirect:/staff?tab=ready";
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Summary summary() {
        var day = queue.getDailySummary();
        List<Request> all = queue.getAllToday();
        long paid = all.stream().filter(r -> r.isPaid() && !r.isFree()).count();
        long oldest = queue.getForSignature().stream().mapToLong(r -> minutesSince(r.getUpdatedAt())).max().orElse(0);
        long avg = Math.round(all.stream().filter(r -> r.getCalledAt() != null)
                .mapToLong(r -> Duration.between(r.getCreatedAt(), r.getCalledAt()).toMinutes()).average().orElse(0));
        long priorityAvg = Math.round(all.stream().filter(r -> r.getCalledAt() != null && r.isPriority())
                .mapToLong(r -> Duration.between(r.getCreatedAt(), r.getCalledAt()).toMinutes()).average().orElse(0));
        return new Summary(day.getTotalRequests(), day.getReleased(), day.getOnHold(), bi.peso(day.getFeesCollected()),
                paid, day.getForSignature(), oldest, avg, priorityAvg);
    }

    private static boolean matches(Request r, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String needle = q.strip().toLowerCase(Locale.ROOT);
        return r.getTicketNumber().toLowerCase(Locale.ROOT).contains(needle)
                || r.getResident().getName().toLowerCase(Locale.ROOT).contains(needle);
    }

    private long minutesSince(LocalDateTime time) {
        return time == null ? 0 : Math.max(0, Duration.between(time, LocalDateTime.now(clock)).toMinutes());
    }

    private void toast(RedirectAttributes flash, String key, Object... args) {
        flash.addFlashAttribute("toast", bi.t(key, args));
    }
}
