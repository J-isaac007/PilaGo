package com.queupid.pilago.web;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Prints copy in English and Filipino side by side. Templates call it as a bean:
 * <pre>
 *   th:text="${@bi.t('cta.next')}"                  → Next · Susunod
 *   th:text="${@bi.t('resident.landing.waiting', 4)}" → 4 waiting · 4 naghihintay
 *   th:text="${@bi.en('resident.step1.tip')}"       → English line only
 *   th:text="${@bi.fil('resident.step1.tip')}"      → Filipino line only
 *   th:text="${@bi.fee(request.fee)}"               → ₱50.00, or Free · Libre
 * </pre>
 * Each key {@code x} needs a twin {@code x.fil} in the messages files.
 */
@Component("bi")
public class Bilingual {

    public static final String SEPARATOR = " · ";

    private final MessageSource messages;

    public Bilingual(MessageSource messages) {
        this.messages = messages;
    }

    /** "English · Filipino" on one line. */
    public String t(String key, Object... args) {
        return en(key, args) + SEPARATOR + fil(key, args);
    }

    public String en(String key, Object... args) {
        return messages.getMessage(key, args, Locale.ROOT);
    }

    public String fil(String key, Object... args) {
        return messages.getMessage(key + ".fil", args, Locale.ROOT);
    }

    /** ₱50.00, or "Free · Libre" when the amount is zero. */
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
