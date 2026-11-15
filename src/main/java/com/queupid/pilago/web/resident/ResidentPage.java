package com.queupid.pilago.web.resident;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import com.queupid.pilago.exception.DuplicateRequestException;
import com.queupid.pilago.exception.NotAResidentException;
import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.document.Purpose;
import com.queupid.pilago.model.person.Resident;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.service.RequestForm;
import com.queupid.pilago.service.RequestQueue;
import com.queupid.pilago.web.Bilingual;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Resident side (public, no login): landing, the three request steps and the ticket hand-off.
 *
 * <pre>
 * GET  /                  R1 / R11 landing
 * GET  /request/document  R2 / R12 step 1, choose document and purpose
 * GET  /request/details   R3 / R13 step 2, your details
 * GET  /request/review    R4 / R14 review
 * POST /request/review    issue the ticket, then go to /ticket/{token}
 * </pre>
 *
 * The answers live in one {@link RequestForm} kept in the session between steps
 * ({@code @SessionAttributes}); it is cleared once the ticket is issued.
 */
@Controller
@SessionAttributes("form")
public class ResidentPage {

    /** Cookie that remembers this phone's latest ticket, so "I have a ticket" can find it. */
    public static final String TICKET_COOKIE = "pilago_ticket";

    private final RequestQueue queue;
    private final Bilingual bi;

    public ResidentPage(RequestQueue queue, Bilingual bi) {
        this.queue = queue;
        this.bi = bi;
    }

    @ModelAttribute("form")
    public RequestForm form() {
        return new RequestForm();
    }

    /**
     * Age is typed as text so seniors can use the phone keypad. Letters are caught here
     * (exception handling from the proposal) and shown as "Use numbers only" under the field.
     */
    @InitBinder("form")
    public void ageAsNumber(WebDataBinder binder) {
        binder.registerCustomEditor(Integer.class, "age", new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.isBlank()) {
                    setValue(null);
                    return;
                }
                try {
                    setValue(Integer.valueOf(text.strip()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Age must be a whole number, got: " + text, e);
                }
            }

            @Override
            public String getAsText() {
                return getValue() == null ? "" : getValue().toString();
            }
        });
    }

    // ── R1 · Landing ─────────────────────────────────────────────────────

    @GetMapping("/")
    public String landing(Model model) {
        List<Request> waiting = queue.getWaiting();
        model.addAttribute("nowServing", queue.getNowServing().map(Request::getTicketNumber).orElse("—"));
        model.addAttribute("waitingCount", waiting.size());
        model.addAttribute("waitMinutes", estimateWaitForNewTicket(waiting));
        model.addAttribute("documents", DocumentType.values());
        model.addAttribute("feeRanges", feeRanges());
        return "resident/landing";
    }

    // ── R2 · Step 1, choose document ─────────────────────────────────────

    @GetMapping("/request/document")
    public String chooseDocument(Model model) {
        addStep1Options(model);
        return "resident/step1";
    }

    @PostMapping("/request/document")
    public String saveDocument(@ModelAttribute("form") RequestForm form, BindingResult result, Model model) {
        if (form.getDocumentType() == null) {
            result.rejectValue("documentType", "form.document.required");
        }
        if (form.getPurpose() == null) {
            result.rejectValue("purpose", "form.purpose.required");
        }
        if (result.hasErrors()) {
            addStep1Options(model);
            return "resident/step1";
        }
        return "redirect:/request/details";
    }

    // ── R3 · Step 2, your details ────────────────────────────────────────

    @GetMapping("/request/details")
    public String details(@ModelAttribute("form") RequestForm form) {
        if (form.getDocumentType() == null || form.getPurpose() == null) {
            return "redirect:/request/document";
        }
        return "resident/step2";
    }

    @PostMapping("/request/details")
    public String saveDetails(@Validated @ModelAttribute("form") RequestForm form, BindingResult result) {
        if (form.getDocumentType() == null || form.getPurpose() == null) {
            return "redirect:/request/document";
        }
        if (result.hasErrors()) {
            return "resident/step2";
        }
        return "redirect:/request/review";
    }

    // ── R4 · Review ──────────────────────────────────────────────────────

    @GetMapping("/request/review")
    public String review(@ModelAttribute("form") RequestForm form, Model model) {
        if (!isComplete(form)) {
            return form.getDocumentType() == null ? "redirect:/request/document" : "redirect:/request/details";
        }
        Resident preview = new Resident(form.getFullName(), form.getAge(), form.getAddress(),
                form.getTotalResidencyMonths(), form.isPwd(), form.isHasPendingRecord());
        boolean willBeOnHold;
        try {
            preview.checkResidency();
            willBeOnHold = false;
        } catch (NotAResidentException e) {
            willBeOnHold = true;
        }
        model.addAttribute("priorityGroup", preview.getPriorityGroup());
        model.addAttribute("willBeOnHold", willBeOnHold);
        model.addAttribute("fee", form.getDocumentType().create(form.getPurpose()).computeFee());
        return "resident/review";
    }

    @PostMapping("/request/review")
    public String submit(@ModelAttribute("form") RequestForm form, SessionStatus session,
            HttpServletResponse response, Model model) {
        if (!isComplete(form)) {
            return "redirect:/request/document";
        }
        try {
            Request ticket = queue.addRequest(form);
            session.setComplete();
            rememberTicket(response, ticket);
            return "redirect:/ticket/" + ticket.getToken();
        } catch (DuplicateRequestException e) {
            session.setComplete();
            model.addAttribute("existing", e.getExisting());
            return "resident/duplicate";
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void addStep1Options(Model model) {
        model.addAttribute("documents", DocumentType.values());
        model.addAttribute("purposes", Purpose.values());
        model.addAttribute("feeRanges", feeRanges());
        model.addAttribute("fees", feeTable());
    }

    /** "₱0–50" or "Free · Libre" for each document, from the documents' own fee rules. */
    private Map<DocumentType, String> feeRanges() {
        Map<DocumentType, String> ranges = new EnumMap<>(DocumentType.class);
        for (DocumentType type : DocumentType.values()) {
            BigDecimal min = null;
            BigDecimal max = null;
            for (Purpose purpose : Purpose.values()) {
                BigDecimal fee = type.create(purpose).computeFee();
                min = min == null || fee.compareTo(min) < 0 ? fee : min;
                max = max == null || fee.compareTo(max) > 0 ? fee : max;
            }
            if (max.signum() == 0) {
                ranges.put(type, bi.fee(BigDecimal.ZERO));
            } else if (min.compareTo(max) == 0) {
                ranges.put(type, bi.peso(max));
            } else {
                ranges.put(type, "₱" + min.stripTrailingZeros().toPlainString() + "–" + max.stripTrailingZeros().toPlainString());
            }
        }
        return ranges;
    }

    /** Exact fee for every document and purpose, keyed "DOCUMENT|PURPOSE", for the live fee box. */
    private Map<String, String> feeTable() {
        Map<String, String> fees = new LinkedHashMap<>();
        for (DocumentType type : DocumentType.values()) {
            for (Purpose purpose : Purpose.values()) {
                fees.put(type.name() + "|" + purpose.name(), bi.fee(type.create(purpose).computeFee()));
            }
        }
        return fees;
    }

    /** Minutes until a ticket taken now would be called. */
    private int estimateWaitForNewTicket(List<Request> waiting) {
        if (waiting.isEmpty()) {
            return queue.getNowServing().map(r -> r.getDocument().getProcessingTime()).orElse(0);
        }
        Request last = waiting.get(waiting.size() - 1);
        return queue.getEstimatedWaitMinutes(last.getTicketNumber()) + last.getDocument().getProcessingTime();
    }

    private static boolean isComplete(RequestForm form) {
        return form.getDocumentType() != null && form.getPurpose() != null
                && form.getFullName() != null && !form.getFullName().isBlank()
                && form.getAge() != null && form.getAddress() != null && !form.getAddress().isBlank()
                && form.getResidencyYears() != null && form.getResidencyMonths() != null;
    }

    private static void rememberTicket(HttpServletResponse response, Request ticket) {
        ResponseCookie cookie = ResponseCookie.from(TICKET_COOKIE, ticket.getToken().toString())
                .path("/").httpOnly(true).sameSite("Lax").maxAge(Duration.ofHours(14)).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
