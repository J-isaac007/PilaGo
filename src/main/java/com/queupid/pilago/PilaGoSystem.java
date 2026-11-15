package com.queupid.pilago;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PilaGo: Barangay Document Ticketing and Queue System.
 *
 * <p>Starts the web app. Residents get tickets at {@code /}, staff log in at
 * {@code /login}, and the TV board runs at {@code /display}.
 */
@SpringBootApplication
public class PilaGoSystem {

    public static void main(String[] args) {
        SpringApplication.run(PilaGoSystem.class, args);
    }
}
