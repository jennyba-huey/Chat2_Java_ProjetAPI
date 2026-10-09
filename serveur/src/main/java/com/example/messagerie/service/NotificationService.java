package com.example.messagerie.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Notification hors ligne (Webhook simule).
 * Quand le destinataire n'est pas connecte au WebSocket, on previent un service tiers
 *  Le message, lui, reste en base et sera lu via l'historique REST.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifierHorsLigne(Long destinataireId, Long expediteurId, Long messageId) {
        log.info("[WEBHOOK] Notification envoyee a l'utilisateur {} : nouveau message {} de l'utilisateur {}",
                destinataireId, messageId, expediteurId);
    }
}