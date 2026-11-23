package com.queupid.pilago.web.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.storage.RequestRepository;

/** Who may open which staff page, the login form, and the secretary → captain → release flow. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pilago-staff-test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StaffRolesTest {

    private static final RequestPostProcessor SECRETARY = user("SEC-001").roles("SECRETARY");
    private static final RequestPostProcessor CAPTAIN = user("CAP-001").roles("CAPTAIN");

    @Autowired
    MockMvc mvc;

    @Autowired
    RequestRepository requests;

    @Autowired
    Clock clock;

    @Test
    void staffPagesNeedALogin() throws Exception {
        mvc.perform(get("/staff")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/captain")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Staff login")));
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/display")).andExpect(status().isOk());
    }

    @Test
    void eachRoleSeesOnlyItsOwnPages() throws Exception {
        mvc.perform(get("/staff").with(SECRETARY)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Call next")))
                .andExpect(content().string(containsString("Maria Santos")));
        mvc.perform(get("/captain").with(SECRETARY)).andExpect(status().isForbidden());

        mvc.perform(get("/captain").with(CAPTAIN)).andExpect(status().isOk())
                .andExpect(content().string(containsString("For signature")));
        mvc.perform(get("/staff").with(CAPTAIN)).andExpect(status().isForbidden());
        mvc.perform(post("/staff/call-next").with(CAPTAIN).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void theLoginPageSendsSomeoneAlreadyLoggedInHome() throws Exception {
        mvc.perform(get("/login").with(SECRETARY)).andExpect(redirectedUrl("/staff"));
        mvc.perform(get("/login").with(CAPTAIN)).andExpect(redirectedUrl("/captain"));
    }

    @Test
    void loginSendsEachRoleHome() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("staffId", "sec-001").param("password", "secretary123"))
                .andExpect(redirectedUrl("/staff"));
        mvc.perform(post("/login").with(csrf()).param("staffId", "CAP-001").param("password", "captain123"))
                .andExpect(redirectedUrl("/captain"));
        mvc.perform(post("/login").with(csrf()).param("staffId", "SEC-001").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void fullFlowFromCounterToRelease() throws Exception {
        // sample day: C-003 (free indigency) is at the counter, S-004 is waiting for the captain
        mvc.perform(post("/staff/call-next").with(SECRETARY).with(csrf()))
                .andExpect(redirectedUrl("/staff"))
                .andExpect(flash().attribute("toastError", containsString("Finish the ticket at the counter first")));

        mvc.perform(get("/staff/request/C-003").with(SECRETARY)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Forward to Captain")));
        mvc.perform(post("/staff/request/C-003/forward").with(SECRETARY).with(csrf()))
                .andExpect(redirectedUrl("/staff"))
                .andExpect(flash().attribute("toast", containsString("C-003 sent to the captain")));

        mvc.perform(post("/captain/sign/C-003").with(CAPTAIN).with(csrf()))
                .andExpect(redirectedUrl("/captain"))
                .andExpect(flash().attribute("toast", containsString("C-003 signed")));
        mvc.perform(get("/staff").param("tab", "ready").with(SECRETARY))
                .andExpect(content().string(containsString("Release to Dela Cruz, Juan P.?")));

        mvc.perform(post("/staff/release/C-003").with(SECRETARY).with(csrf()))
                .andExpect(redirectedUrl("/staff?tab=ready"))
                .andExpect(flash().attribute("toast", containsString("C-003 released")));

        // next in line is P-005 (PWD), a paid-free residency certificate
        mvc.perform(post("/staff/call-next").with(SECRETARY).with(csrf()))
                .andExpect(flash().attribute("toast", containsString("P-005 called")));
    }

    @Test
    void staffActionsSaveAndDisplayTheirAttribution() throws Exception {
        mvc.perform(post("/staff/request/C-003/forward").with(SECRETARY).with(csrf()))
                .andExpect(redirectedUrl("/staff"));
        mvc.perform(post("/staff/call-next").with(SECRETARY).with(csrf()))
                .andExpect(flash().attribute("toast", containsString("P-005 called")));
        mvc.perform(post("/staff/request/P-005/forward").with(SECRETARY).with(csrf()))
                .andExpect(redirectedUrl("/staff"));

        mvc.perform(get("/captain").param("view", "P-005").with(CAPTAIN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Called by")))
                .andExpect(content().string(containsString("Processed by")))
                .andExpect(content().string(containsString("Maria Santos ·")));

        mvc.perform(post("/captain/sign/P-005").with(CAPTAIN).with(csrf()))
                .andExpect(redirectedUrl("/captain"));
        mvc.perform(post("/captain/sign-all").with(CAPTAIN).with(csrf()))
                .andExpect(redirectedUrl("/captain"));
        mvc.perform(get("/staff/request/P-005").with(SECRETARY))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Signed by")))
                .andExpect(content().string(containsString("Roberto Mendoza ·")));

        var saved = requests.findByTicketDateOrderByDailySequenceAsc(LocalDate.now(clock));
        Request ticket = saved.stream().filter(r -> r.getTicketNumber().equals("P-005")).findFirst().orElseThrow();
        assertThat(ticket.getCalledBy()).isEqualTo("Maria Santos");
        assertThat(ticket.getForwardedBy()).isEqualTo("Maria Santos");
        assertThat(ticket.getSignedBy()).contains("Roberto Mendoza");
        assertThat(ticket.getCalledAt()).isNotNull();
        assertThat(ticket.getForwardedAt()).isAfterOrEqualTo(ticket.getCalledAt());
        assertThat(ticket.getSignedAt()).isAfterOrEqualTo(ticket.getForwardedAt());
        assertThat(saved).filteredOn(r -> r.getTicketNumber().equals("C-003") || r.getTicketNumber().equals("S-004"))
                .hasSize(2).allSatisfy(r -> {
                    assertThat(r.getSignedBy()).contains("Roberto Mendoza");
                    assertThat(r.getSignedAt()).isNotNull();
                });
    }

    @Test
    void unpaidClearanceCannotBeForwardedAndHoldNeedsAReason() throws Exception {
        // finish C-003 so the next ticket can be called
        mvc.perform(post("/staff/request/C-003/forward").with(SECRETARY).with(csrf()));
        mvc.perform(post("/staff/call-next").with(SECRETARY).with(csrf())); // P-005
        mvc.perform(post("/staff/request/P-005/forward").with(SECRETARY).with(csrf()));
        mvc.perform(post("/staff/call-next").with(SECRETARY).with(csrf())); // S-008
        mvc.perform(post("/staff/request/S-008/forward").with(SECRETARY).with(csrf()));
        mvc.perform(post("/staff/call-next").with(SECRETARY).with(csrf())); // C-006, clearance for a job: ₱50

        mvc.perform(post("/staff/request/C-006/forward").with(SECRETARY).with(csrf()))
                .andExpect(redirectedUrl("/staff/request/C-006"))
                .andExpect(flash().attribute("toastError", containsString("not allowed")));
        mvc.perform(post("/staff/request/C-006/hold").with(SECRETARY).with(csrf())
                        .param("reason", "MISSING_REQUIREMENT").param("note", "Bring a valid ID"))
                .andExpect(redirectedUrl("/staff?tab=on-hold"));
        mvc.perform(get("/staff").param("tab", "on-hold").with(SECRETARY))
                .andExpect(content().string(containsString("C-006")));
        mvc.perform(post("/staff/request/C-006/verify").with(SECRETARY).with(csrf()))
                .andExpect(flash().attribute("toast", containsString("C-006 is back in line")));
    }

    @Test
    void wrongPagesShowFriendlyErrors() throws Exception {
        mvc.perform(get("/staff/request/Z-999").with(SECRETARY)).andExpect(status().isNotFound());
        mvc.perform(get("/no-such-page")).andExpect(status().isNotFound());
    }
}
