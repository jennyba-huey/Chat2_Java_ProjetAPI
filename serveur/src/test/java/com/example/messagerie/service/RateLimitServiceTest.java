package com.example.messagerie.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    @Test
    void refuseLe11eMessageEn10Secondes() {
        RateLimitService service = new RateLimitService(10, 10_000);
        for (int i = 1; i <= 10; i++) {
            assertTrue(service.autoriser(1L), "Le message " + i + " doit passer");
        }
        assertFalse(service.autoriser(1L), "Le 11e message doit etre refuse");
    }

    @Test
    void chaqueUtilisateurASaPropreLimite() {
        RateLimitService service = new RateLimitService(10, 10_000);
        for (int i = 0; i < 10; i++) {
            service.autoriser(1L);
        }
        assertFalse(service.autoriser(1L), "Alice est bloquee");
        assertTrue(service.autoriser(2L), "Bob n'est pas bloque par les messages d'Alice");
    }

    @Test
    void debloqueApresLaFenetre() throws InterruptedException {
        RateLimitService service = new RateLimitService(10, 200); // fenetre courte pour le test
        for (int i = 0; i < 10; i++) {
            service.autoriser(1L);
        }
        assertFalse(service.autoriser(1L), "Bloque pendant la fenetre");
        Thread.sleep(250);
        assertTrue(service.autoriser(1L), "Debloque une fois la fenetre passee");
    }
}