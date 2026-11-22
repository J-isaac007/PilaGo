package com.queupid.pilago.web.staff;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** P4 · /staff/report.csv: today's tickets as a downloadable file. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pilago-report-test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StaffReportTest {

    private static final RequestPostProcessor SECRETARY = user("SEC-001").roles("SECRETARY");
    private static final RequestPostProcessor CAPTAIN = user("CAP-001").roles("CAPTAIN");

    @Autowired
    MockMvc mvc;

    @Test
    void secretaryDownloadsTodaysTickets() throws Exception {
        // the sample day has C-001 released: Aquino, Pedro T., clearance for employment, paid ₱50
        mvc.perform(get("/staff/report.csv").with(SECRETARY))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"pilago-report-")))
                .andExpect(content().string(startsWith(
                        "\uFEFFticket,name,document,purpose,status,fee,paid,issued,called,released\r\n")))
                .andExpect(content().string(containsString(
                        "C-001,\"Aquino, Pedro T.\",\"Barangay Clearance\",\"Employment\",RELEASED,50.00,true,")));
    }

    @Test
    void onlyTheSecretaryMayDownload() throws Exception {
        mvc.perform(get("/staff/report.csv").with(CAPTAIN)).andExpect(status().isForbidden());
        mvc.perform(get("/staff/report.csv")).andExpect(redirectedUrl("/login"));
    }
}
