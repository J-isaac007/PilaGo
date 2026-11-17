package com.queupid.pilago.web.captain;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.queupid.pilago.model.person.StaffAction;
import com.queupid.pilago.model.person.StaffMember;
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
        model.addAttribute("selected", view == null ? null
                : toSign.stream().filter(r -> r.getTicketNumber().equalsIgnoreCase(view)).findFirst().orElse(null));
        return "captain/list";
    }

    @PostMapping("/sign/{ticket}")
    public String sign(@PathVariable String ticket, Principal principal, RedirectAttributes flash) {
        current.require(principal, StaffAction.SIGN);
        queue.sign(ticket);
        flash.addFlashAttribute("toast", bi.t("captain.toast.signed", ticket));
        return "redirect:/captain";
    }

    @PostMapping("/sign-all")
    public String signAll(Principal principal, RedirectAttributes flash) {
        current.require(principal, StaffAction.SIGN);
        List<Request> toSign = queue.getForSignature();
        for (Request r : toSign) {
            queue.sign(r.getTicketNumber());
        }
        flash.addFlashAttribute("toast", bi.t("captain.toast.signed-all", toSign.size()));
        return "redirect:/captain";
    }
}
