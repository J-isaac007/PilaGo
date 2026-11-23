package com.queupid.pilago.web.captain;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.queupid.pilago.model.person.StaffAction;
import com.queupid.pilago.model.person.StaffMember;
import com.queupid.pilago.model.request.HoldReason;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.service.RequestQueue;
import com.queupid.pilago.web.Bilingual;
import com.queupid.pilago.web.staff.CurrentStaff;

/**
 * The captain's page (S4 / S5 / S11 / S12).
 *
 * <pre>
 * GET  /captain               requests waiting for a signature; ?view=S-004 opens the details panel
 * POST /captain/sign/{ticket} FOR_SIGNATURE → READY
 * POST /captain/sign-all      sign every request in the list
 * POST /captain/return/{ticket}  FOR_SIGNATURE → ON_HOLD, with a reason (and a note for "Other")
 * </pre>
 */
@Controller
@RequestMapping("/captain")
public class CaptainPage {

    private final RequestQueue queue;
    private final CurrentStaff current;
    private final Bilingual bi;

    public CaptainPage(RequestQueue queue, CurrentStaff current, Bilingual bi) {
        this.queue = queue;
        this.current = current;
        this.bi = bi;
    }

    @ModelAttribute("me")
    public StaffMember me(Principal principal) {
        return current.get(principal);
    }

    @GetMapping
    public String list(@RequestParam(required = false) String view, Principal principal, Model model) {
        current.require(principal, StaffAction.VIEW_FOR_SIGNATURE);
        List<Request> toSign = queue.getForSignature();
        model.addAttribute("requests", toSign);
        model.addAttribute("returnReasons", HoldReason.returnReasons());
        model.addAttribute("selected", view == null ? null
                : toSign.stream().filter(r -> r.getTicketNumber().equalsIgnoreCase(view)).findFirst().orElse(null));
        return "captain/list";
    }

    @PostMapping("/sign/{ticket}")
    public String sign(@PathVariable String ticket, Principal principal, RedirectAttributes flash) {
        StaffMember member = current.require(principal, StaffAction.SIGN);
        queue.sign(ticket, member.getName());
        flash.addFlashAttribute("toast", bi.t("captain.toast.signed", ticket));
        return "redirect:/captain";
    }

    /**
     * P1 · The captain sends a request back to the secretary instead of signing it. The resident sees the
     * reason on their ticket. Only the captain may do this (StaffAction.RETURN_TO_SECRETARY).
     */
    @PostMapping("/return/{ticket}")
    public String returnToSecretary(@PathVariable String ticket, @RequestParam HoldReason reason,
            @RequestParam(defaultValue = "") String note, Principal principal, RedirectAttributes flash) {
        current.require(principal, StaffAction.RETURN_TO_SECRETARY);
        if (!HoldReason.returnReasons().contains(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The captain cannot return with " + reason);
        }
        String trimmed = note.strip();
        queue.returnToSecretary(ticket, reason, trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed);
        flash.addFlashAttribute("toast", bi.t("captain.toast.returned", ticket));
        return "redirect:/captain";
    }

    @PostMapping("/sign-all")
    public String signAll(Principal principal, RedirectAttributes flash) {
        StaffMember member = current.require(principal, StaffAction.SIGN);
        List<Request> toSign = queue.getForSignature();
        for (Request r : toSign) {
            queue.sign(r.getTicketNumber(), member.getName());
        }
        flash.addFlashAttribute("toast", bi.t("captain.toast.signed-all", toSign.size()));
        return "redirect:/captain";
    }
}
