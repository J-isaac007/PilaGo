package com.queupid.pilago.web.qr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.service.RequestQueue;

/** C3 · The two QR images: what they open, and that a wrong ticket link gets 404. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pilago-qr-test",
        "pilago.public-url=pilago.onrender.com" })
@AutoConfigureMockMvc
class QrPageTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    RequestQueue queue;

    /** Fetches a QR image and reads it back, the way a phone camera would. */
    private String scan(String url) throws Exception {
        byte[] png = mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        return new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image))))
                .getText();
    }

    @Test
    void theTvCodeOpensPilaGo() throws Exception {
        assertThat(scan("/qr/home.png")).isEqualTo("https://pilago.onrender.com/");
    }

    @Test
    void theTicketCodeOpensThatSameTicket() throws Exception {
        Request ticket = queue.findByTicket("S-004").orElseThrow(); // from the sample day
        assertThat(scan("/ticket/" + ticket.getToken() + "/qr.png"))
                .isEqualTo("https://pilago.onrender.com/ticket/" + ticket.getToken());
    }

    @Test
    void anUnknownOrMadeUpTicketGets404() throws Exception {
        mvc.perform(get("/ticket/00000000-0000-0000-0000-000000000000/qr.png")).andExpect(status().isNotFound());
        mvc.perform(get("/ticket/not-a-ticket/qr.png")).andExpect(status().isNotFound());
    }

    @Test
    void theTicketPageAndTheBoardShowTheirCodes() throws Exception {
        Request ticket = queue.findByTicket("S-004").orElseThrow();
        mvc.perform(get("/ticket/" + ticket.getToken()))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/ticket/" + ticket.getToken() + "/qr.png")));
        mvc.perform(get("/display"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/qr/home.png")));
    }
}
