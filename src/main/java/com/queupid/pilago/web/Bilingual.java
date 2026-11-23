package com.queupid.pilago.web;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Prints page text from the messages files. Templates call it as a bean:
 * <pre>
 *   th:text="${@bi.t('cta.next')}"                    → Next
 *   th:text="${@bi.t('resident.landing.waiting', 4)}" → 4 waiting
 *   th:text="${@bi.fee(request.fee)}"                 → ₱50.00, or Free
 * </pre>
 * The app was English and Filipino side by side until 2026-11-23; it is English-only now, matching
 * the Figma. The bean keeps its short name {@code bi} so the templates did not have to change.
 */
@Component("bi")
public class Bilingual {

    private final MessageSource messages;

    public Bilingual(MessageSource messages) {
        this.messages = messages;
    }

    /** The text for a key, with any {0}, {1}… filled in. */
    public String t(String key, Object... args) {
        return messages.getMessage(key, args, Locale.ROOT);
    }

    /** ₱50.00, or "Free" when the amount is zero. */
    public String fee(BigDecimal amount) {
        if (amount == null || amount.signum() == 0) {
            return t("fee.free");
        }
        return peso(amount);
    }

    /** ₱1,250.00 */
    public String peso(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return "₱" + format.format(amount == null ? BigDecimal.ZERO : amount);
    }
}
