package com.queupid.pilago.web.captain;

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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.queupid.pilago.model.request.HoldReason;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.model.request.RequestStatus;
import com.queupid.pilago.service.RequestQueue;

/** P1 · The captain sends a request back to the secretary (Figma S4 / S4B / S11 / S11b). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pilago-return-test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CaptainReturnTest {

    private static final RequestPostProcessor SECRETARY = user("SEC-001").roles("SECRETARY");
    private static final RequestPostProcessor CAPTAIN = user("CAP-001").roles("CAPTAIN");

    @Autowired
    MockMvc mvc;

    @Autowired
    RequestQueue queue;

    /** The sample day has S-004 waiting for the captain's signature. */
    private Request s004() {
        return queue.findByTicket("S-004").orElseThrow();
    }

    @Test
    void captainSeesTheReturnButtonOnTheListAndTheDetails() throws Exception {
        mvc.perform(get("/captain").with(CAPTAIN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Return to secretary")))
                .andExpect(content().string(containsString("Wrong details")));
        mvc.perform(get("/captain").param("view", "S-004").with(CAPTAIN))
                .andExpect(content().string(containsString("dlg-return-detail")));
    }

    @Test
    void captainReturnsARequestAndTheResidentSeesTheReason() throws Exception {
        mvc.perform(post("/captain/return/S-004").with(CAPTAIN).with(csrf())
                        .param("reason", "WRONG_DETAILS").param("note", "Check the house number"))
                .andExpect(redirectedUrl("/captain"))
                .andExpect(flash().attribute("toast", "S-004 returned to the secretary."));

        Request request = s004();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.ON_HOLD);
        assertThat(request.getHoldReason()).isEqualTo(HoldReason.WRONG_DETAILS);
        assertThat(request.getHoldNote()).isEqualTo("Check the house number");
        assertThat(request.isReturnedByCaptain()).isTrue();

        mvc.perform(get("/ticket/" + request.getToken()))
                .andExpect(content().string(containsString("Some of your details need to be corrected")))
                .andExpect(content().string(containsString("Check the house number")));
        mvc.perform(get("/staff/request/S-004").with(SECRETARY))
                .andExpect(content().string(containsString("Returned by the captain")));
    }

    @Test
    void theSecretaryCannotReturnARequest() throws Exception {
        mvc.perform(post("/captain/return/S-004").with(SECRETARY).with(csrf())
                        .param("reason", "WRONG_DETAILS"))
                .andExpect(status().isForbidden());
        assertThat(s004().getStatus()).isEqualTo(RequestStatus.FOR_SIGNATURE);
    }

    @Test
    void otherNeedsANote() throws Exception {
        mvc.perform(post("/captain/return/S-004").with(CAPTAIN).with(csrf())
                        .param("reason", "OTHER").param("note", "   "))
                .andExpect(redirectedUrl("/captain"))
                .andExpect(flash().attribute("toastError",
                        "Add a note that says what to fix when the reason is Other."));
        assertThat(s004().getStatus()).isEqualTo(RequestStatus.FOR_SIGNATURE);

        mvc.perform(post("/captain/return/S-004").with(CAPTAIN).with(csrf())
                        .param("reason", "OTHER").param("note", "Photo is blurry"))
                .andExpect(flash().attribute("toast", "S-004 returned to the secretary."));
        assertThat(s004().getStatus()).isEqualTo(RequestStatus.ON_HOLD);
    }

    @Test
    void aReasonOnlyTheSecretaryUsesIsRefused() throws Exception {
        mvc.perform(post("/captain/return/S-004").with(CAPTAIN).with(csrf())
                        .param("reason", "SHORT_RESIDENCY"))
                .andExpect(status().isBadRequest());
        assertThat(s004().getStatus()).isEqualTo(RequestStatus.FOR_SIGNATURE);
    }

    @Test
    void aRequestThatIsNotWaitingForSignatureCannotBeReturned() throws Exception {
        // C-001 was already released in the sample day
        mvc.perform(post("/captain/return/C-001").with(CAPTAIN).with(csrf())
                        .param("reason", "WRONG_DETAILS"))
                .andExpect(redirectedUrl("/captain"))
                .andExpect(flash().attributeExists("toastError"));
        assertThat(queue.findByTicket("C-001").orElseThrow().getStatus()).isEqualTo(RequestStatus.RELEASED);
    }

    @Test
    void nobodyWithoutALoginCanReturnARequest() throws Exception {
        mvc.perform(post("/captain/return/S-004").with(csrf()).param("reason", "WRONG_DETAILS"))
                .andExpect(redirectedUrl("/login"));
    }
}
