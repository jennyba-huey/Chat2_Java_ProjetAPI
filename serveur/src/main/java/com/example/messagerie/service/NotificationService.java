package com.example.messagerie.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifierHorsLigne(Long destinataireId, Long expediteurId, Long messageId) {
        log.info("[WEBHOOK] Notification envoyee a l'utilisateur {} : nouveau message {} de l'utilisateur {}",
                destinataireId, messageId, expediteurId);
    }
}