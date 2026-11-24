package com.queupid.pilago.web.staff;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

import com.queupid.pilago.model.person.StaffMember;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.model.request.RequestStatus;

/**
 * Turns a list of requests into CSV text (the file Excel and Google Sheets open).
 * One line per ticket, built with a loop over the list.
 *
 * <p>Two rules keep the file clean: a value with a comma, such as "Santos, Ana R.", is wrapped in quotes so
 * it stays in one column; and a value that starts with {@code = + - @} gets a leading apostrophe so a
 * spreadsheet never runs it as a formula.
 */
public class ReportCsv {

    public static final String HEADER = "ticket,name,document,purpose,status,fee,paid,issued,called,released,"
            + "called_by,payment_received_by,forwarded_by,returned_by,signed_by,released_by";

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Looks up English text for a message key, e.g. purpose.employment → Employment. */
    private final Function<String, String> englishText;

    public ReportCsv(Function<String, String> englishText) {
        this.englishText = englishText;
    }

    public String render(List<Request> requests) {
        // The first character is a UTF-8 marker that helps Excel read accents and the peso sign correctly.
        StringBuilder csv = new StringBuilder("\uFEFF").append(HEADER).append("\r\n");
        for (Request request : requests) {
            // Tickets released before the release time was recorded: RELEASED is a final status, so their last
            // update is the release time.
            LocalDateTime released = request.getReleasedAt() != null ? request.getReleasedAt()
                    : request.getStatus() == RequestStatus.RELEASED ? request.getUpdatedAt() : null;
            csv.append(String.join(",",
                    request.getTicketNumber(),
                    text(request.getResident().getName()),
                    text(request.getDocumentType().getEnglishName()),
                    text(englishText.apply(request.getPurpose().getMessageKey())),
                    request.getStatus().name(),
                    request.getFee().toPlainString(),
                    Boolean.toString(request.isPaid()),
                    time(request.getCreatedAt()),
                    time(request.getCalledAt()),
                    time(released),
                    staffId(request.getCalledBy()),
                    staffId(request.getPaymentReceivedBy()),
                    staffId(request.getForwardedBy()),
                    staffId(request.getReturnedBy()),
                    staffId(request.getSignedBy()),
                    staffId(request.getReleasedBy()))).append("\r\n");
        }
        return csv.toString();
    }

    private static String time(LocalDateTime time) {
        return time == null ? "" : time.format(TIME);
    }

    /** The Staff ID (e.g. SEC-001), which stays the same even if a name changes; empty if nobody. */
    private static String staffId(StaffMember staff) {
        return staff == null ? "" : text(staff.getStaffId());
    }

    private static String text(String value) {
        String start = value.stripLeading();
        if (!start.isEmpty() && "=+-@".indexOf(start.charAt(0)) >= 0) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
