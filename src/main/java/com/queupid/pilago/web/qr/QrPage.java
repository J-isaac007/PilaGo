package com.queupid.pilago.web.qr;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.service.RequestQueue;

/**
 * QR code images, served from our own endpoints only.
 * 
 * <pre>
 * GET /qr/home.png              opens PilaGo (TV footer)
 * GET /ticket/{token}/qr.png    opens that ticket on another phone
 * </pre>
 */
@Controller
public class QrPage {

    private static final int SIZE = 300;

    private final RequestQueue queue;
    private final String publicUrl;

    public QrPage(RequestQueue queue,
            @Value("${pilago.public-url:pilago-sanisidro.onrender.com}") String publicUrl) {
        this.queue = queue;
        this.publicUrl = publicUrl;
    }

    @GetMapping("/qr/home.png")
    public ResponseEntity<byte[]> home() {
        return png(baseUrl() + "/");
    }

    @GetMapping("/ticket/{token}/qr.png")
    public ResponseEntity<byte[]> ticket(@PathVariable String token) {
        Optional<Request> ticket = find(token);
        if (ticket.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // Built from the stored token, never from the text in the request URL.
        return png(baseUrl() + "/ticket/" + ticket.get().getToken());
    }

    private ResponseEntity<byte[]> png(String target) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(QrCodes.png(target, SIZE));
    }

    private Optional<Request> find(String token) {
        try {
            return queue.findByToken(UUID.fromString(token));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** The public address with a scheme and no trailing slash. */
    private String baseUrl() {
        String url = publicUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
