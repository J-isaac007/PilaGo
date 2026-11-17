package com.queupid.pilago.exception;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.queupid.pilago.web.Bilingual;
import com.queupid.pilago.web.captain.CaptainPage;
import com.queupid.pilago.web.staff.StaffPage;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Turns queue rule errors on the staff and captain pages into a red toast instead of an error page.
 * Example: pressing Call next while a ticket is still at the counter throws {@link QueueBusyException};
 * the secretary sees "Finish the ticket at the counter first · Tapusin muna…" and stays on the dashboard.
 *
 * <p>Anything else (wrong role, missing page, a bug) falls through to the bilingual error pages in
 * {@code templates/error/}.
 */
@ControllerAdvice(assignableTypes = { StaffPage.class, CaptainPage.class })
public class GlobalExceptionHandler {

    private static final Pattern REQUEST_PAGE = Pattern.compile("^/staff/request/([^/]+)/");

    private final Bilingual bi;

    public GlobalExceptionHandler(Bilingual bi) {
        this.bi = bi;
    }

    @ExceptionHandler(PilaGoException.class)
    public String queueRuleBroken(PilaGoException e, HttpServletRequest request) {
        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("toastError", bi.t(e.getMessageKey()));
        return "redirect:" + backTo(request);
    }

    /** Back to the page the button was on. */
    private static String backTo(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/captain")) {
            return "/captain";
        }
        Matcher m = REQUEST_PAGE.matcher(path);
        if (m.find()) {
            return "/staff/request/" + m.group(1);
        }
        return "/staff";
    }
}
