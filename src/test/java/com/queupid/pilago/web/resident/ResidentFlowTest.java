package com.queupid.pilago.web.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Resident pages and the TV board, end to end against the sample queue. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pilago-web-test")
@AutoConfigureMockMvc
class ResidentFlowTest {

    @Autowired
    MockMvc mvc;

    @Test
    void publicPagesLoad() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Get a ticket · Kumuha ng tiket")));
        mvc.perform(get("/request/document")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Certificate of Residency")));
        mvc.perform(get("/display")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Go to Counter 1 · Pumunta sa Counter 1")));
        mvc.perform(get("/display/board")).andExpect(status().isOk());
        mvc.perform(get("/ticket")).andExpect(status().isOk()).andExpect(view().name("resident/find-ticket"));
    }

    @Test
    void fullRequestIssuesATicket() throws Exception {
        MockHttpSession session = new MockHttpSession();
        chooseDocument(session, "CERTIFICATE_OF_RESIDENCY", "SCHOOL");
        mvc.perform(post("/request/details").session(session).with(csrf())
                        .param("fullName", "Lim, Rosa T.").param("age", "70").param("address", "8 Mabini St.")
                        .param("residencyYears", "12").param("residencyMonths", "3"))
                .andExpect(redirectedUrl("/request/review"));
        mvc.perform(get("/request/review").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Your ticket starts with S")));

        MvcResult issued = mvc.perform(post("/request/review").session(session).with(csrf()))
                .andExpect(redirectedUrlPattern("/ticket/*"))
                .andExpect(cookie().exists(ResidentPage.TICKET_COOKIE))
                .andReturn();

        String ticketUrl = issued.getResponse().getRedirectedUrl();
        String page = mvc.perform(get(ticketUrl)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        assertThat(page).contains("S-0").contains("Waiting · Naghihintay");
        mvc.perform(get(ticketUrl + "/live")).andExpect(status().isOk());
    }

    @Test
    void lettersInAgeAreExplained() throws Exception {
        MockHttpSession session = new MockHttpSession();
        chooseDocument(session, "BARANGAY_CLEARANCE", "EMPLOYMENT");
        mvc.perform(post("/request/details").session(session).with(csrf())
                        .param("fullName", "Cruz, Ana").param("age", "sixty").param("address", "Purok 2")
                        .param("residencyYears", "1").param("residencyMonths", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("resident/step2"))
                .andExpect(content().string(containsString("Use numbers only")));
    }

    @Test
    void secondTicketForSameDocumentShowsTheFirst() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            MockHttpSession session = new MockHttpSession();
            chooseDocument(session, "CERTIFICATE_OF_INDIGENCY", "MEDICAL");
            mvc.perform(post("/request/details").session(session).with(csrf())
                    .param("fullName", "Ramos, Ben").param("age", "40").param("address", "Purok 5")
                    .param("residencyYears", "3").param("residencyMonths", "0"));
            MvcResult result = mvc.perform(post("/request/review").session(session).with(csrf())).andReturn();
            if (attempt == 1) {
                assertThat(result.getModelAndView().getViewName()).isEqualTo("resident/duplicate");
            }
        }
    }

    @Test
    void stepsCannotBeSkipped() throws Exception {
        mvc.perform(get("/request/review")).andExpect(redirectedUrl("/request/document"));
        mvc.perform(get("/request/details")).andExpect(redirectedUrl("/request/document"));
        mvc.perform(post("/request/document").with(csrf())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose a document")));
    }

    @Test
    void badTicketLinkIsNotFound() throws Exception {
        mvc.perform(get("/ticket/not-a-ticket")).andExpect(status().isNotFound());
        mvc.perform(get("/ticket/2f1c3e0a-0000-4000-8000-000000000000")).andExpect(status().isNotFound());
    }

    private void chooseDocument(MockHttpSession session, String document, String purpose) throws Exception {
        mvc.perform(post("/request/document").session(session).with(csrf())
                        .param("documentType", document).param("purpose", purpose))
                .andExpect(redirectedUrl("/request/details"));
    }
}
