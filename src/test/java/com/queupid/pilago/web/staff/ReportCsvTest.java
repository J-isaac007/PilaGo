package com.queupid.pilago.web.staff;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.document.Purpose;
import com.queupid.pilago.model.person.Resident;
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
        assertThat(lines[0]).isEqualTo("\uFEFFticket,name,document,purpose,status,fee,paid,issued,called,released");
        assertThat(lines[1]).isEqualTo(
                "C-001,\"Santos, Ana R.\",\"Barangay Clearance\",\"Employment\",WAITING,50.00,false,2026-11-16 09:00:00,,");
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
