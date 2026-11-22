package com.queupid.pilago.web.staff;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.queupid.pilago.config.SecurityConfig;

/**
 * S1 / S8 · Staff login. Spring Security handles {@code POST /login} and {@code POST /logout};
 * this controller only shows the form.
 */
@Controller
public class LoginPage {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, @RequestParam(required = false) String logout,
            @RequestParam(required = false) String locked,
            Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            boolean captain = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + SecurityConfig.CAPTAIN));
            return captain ? "redirect:/captain" : "redirect:/staff";
        }
        model.addAttribute("loginError", error != null);
        model.addAttribute("locked", locked != null);
        model.addAttribute("loggedOut", logout != null);
        return "auth/login";
    }
}
