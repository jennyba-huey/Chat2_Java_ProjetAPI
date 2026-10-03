package com.example.messagerie.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitation de debit (anti-spam) sur l'envoi de messages.
 * Un utilisateur ne peut pas envoyer plus de maxMessages messages
 * sur une fenetre glissante de fenetreMs millisecondes (par defaut : 10 messages en 10 secondes).
 *
 * Utilisation cote WebSocket (ChatWebSocketHandler), avant d'enregistrer un message :
 *   if (!rateLimitService.autoriser(expediteurId)) {
 *       // renvoyer {"type":"erreur","message":"Trop de messages, patientez"}
 *       return;
 *   }
 */
@Service
public class RateLimitService {

    private final int maxMessages;
    private final long fenetreMs;

    /** Pour chaque utilisateur : les heures (en ms) de ses envois recents, du plus ancien au plus recent. */
    private final Map<Long, Deque<Long>> envoisRecents = new ConcurrentHashMap<>();

    public RateLimitService(@Value("${ratelimit.max-messages:10}") int maxMessages,
                            @Value("${ratelimit.fenetre-ms:10000}") long fenetreMs) {
        this.maxMessages = maxMessages;
        this.fenetreMs = fenetreMs;
    }

    /**
     * Renvoie true si l'utilisateur peut envoyer un message maintenant (et enregistre cet envoi),
     * false s'il a deja atteint la limite sur la fenetre en cours.
     */
    public boolean autoriser(Long utilisateurId) {
        long maintenant = System.currentTimeMillis();
        Deque<Long> envois = envoisRecents.computeIfAbsent(utilisateurId, id -> new ArrayDeque<>());

        synchronized (envois) {
            // 1. On oublie les envois sortis de la fenetre
            while (!envois.isEmpty() && maintenant - envois.peekFirst() >= fenetreMs) {
                envois.pollFirst();
            }
            // 2. Limite atteinte : on refuse
            if (envois.size() >= maxMessages) {
                return false;
            }
            // 3. Sinon on accepte et on note cet envoi
            envois.addLast(maintenant);
            return true;
        }
    }
}