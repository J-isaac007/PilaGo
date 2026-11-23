
package com.queupid.pilago.web.resident;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Resident side (public, no login).
 * R1 landing, R2 step 1 (choose document), R3 step 2 (your details).
 * R4 review and the ticket pages come next.
 */
@Controller
public class ResidentPage {

    private static final List<String> PURPOSES = List.of(
            "Employment / job application", "School", "Loan", "Medical or financial aid", "Others");

    /**
     * R1 · Landing. The teaser numbers are sample data until RequestQueue is wired
     * in.
     */
    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("nowServing", "C-003"); // later: RequestQueue.getNowServing()
        model.addAttribute("waiting", 12); // later: RequestQueue.getWaiting().size()
        model.addAttribute("waitMinutes", 25);
        return "resident/landing";
    }

    /** R2 · Step 1 */
    @GetMapping("/request/document")
    public String chooseDocument(Model model) {
        model.addAttribute("purposes", PURPOSES);
        return "resident/step1";
    }

    /** R3 · Step 2. Step 1 sends the choice as ?doc=...&purpose=... */
    @GetMapping("/request/details")
    public String details(@RequestParam(defaultValue = "CLEARANCE") String doc,
            @RequestParam(defaultValue = "Employment / job application") String purpose,
            Model model) {
        model.addAttribute("doc", doc);
        model.addAttribute("purpose", purpose);
        return "resident/step2";
    }
}