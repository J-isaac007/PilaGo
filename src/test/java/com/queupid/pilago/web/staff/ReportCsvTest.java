package com.queupid.pilago.web.staff;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.document.Purpose;
import com.queupid.pilago.model.person.BarangayCaptain;
import com.queupid.pilago.model.person.BarangaySecretary;
import com.queupid.pilago.model.person.Resident;
import com.queupid.pilago.model.person.StaffMember;
import com.queupid.pilago.model.request.Request;

class ReportCsvTest {

    private final ReportCsv csv = new ReportCsv(key -> "Employment");

    private static Request ticket(String name) {
        Resident resident = new Resident(name, 30, "Alangilan", 24, false, false);
        return new Request(resident, DocumentType.BARANGAY_CLEARANCE, Purpose.EMPLOYMENT,
                LocalDate.of(2026, 11, 16), 1, LocalDateTime.of(2026, 11, 16, 9, 0));
    }

    @Test
    void hasTheHeaderAndOneRowPerTicket() {
        String[] lines = csv.render(List.of(ticket("Santos, Ana R."))).split("\r\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[0]).isEqualTo("\uFEFFticket,name,document,purpose,status,fee,paid,issued,called,released,"
                + "called_by,payment_received_by,forwarded_by,returned_by,signed_by,released_by");
        assertThat(lines[1]).isEqualTo(
                "C-001,\"Santos, Ana R.\",\"Barangay Clearance\",\"Employment\",WAITING,50.00,false,2026-11-16 09:00:00,,,,,,,,");
    }

    @Test
    void showsTheStaffIdOfWhoDidEachStep() {
        StaffMember secretary = new BarangaySecretary("Maria Santos", 35, "Barangay Hall", "SEC-001", "hash");
        StaffMember captain = new BarangayCaptain("Roberto Mendoza", 50, "Barangay Hall", "CAP-001", "hash");
        LocalDateTime nine = LocalDateTime.of(2026, 11, 16, 9, 0);
        Request request = ticket("Cruz, Ben");
        request.call(secretary, nine.plusMinutes(5));
        request.markAsPaid(secretary, nine.plusMinutes(6));
        request.forwardToCaptain(secretary, nine.plusMinutes(7));
        request.sign(captain, nine.plusMinutes(20));
        request.release(secretary, nine.plusMinutes(30));

        String row = csv.render(List.of(request)).split("\r\n")[1];
        assertThat(row).endsWith(",RELEASED,50.00,true,2026-11-16 09:00:00,2026-11-16 09:05:00,2026-11-16 09:30:00,"
                + "\"SEC-001\",\"SEC-001\",\"SEC-001\",,\"CAP-001\",\"SEC-001\"");
    }

    @Test
    void aNameWithACommaStaysInOneColumn() {
        String row = csv.render(List.of(ticket("Santos, Ana R."))).split("\r\n")[1];
        assertThat(row).contains(",\"Santos, Ana R.\",");
    }

    @Test
    void quotesAreDoubledAndFormulasAreDefused() {
        String row = csv.render(List.of(ticket("=SUM(1,1) \"x\""))).split("\r\n")[1];
        assertThat(row).contains(",\"'=SUM(1,1) \"\"x\"\"\",");
    }

    @Test
    void anEmptyDayStillHasTheHeader() {
        assertThat(csv.render(List.of())).isEqualTo("\uFEFF" + ReportCsv.HEADER + "\r\n");
    }
}
