package com.example.messagerie.websocket;

import com.example.messagerie.dto.MessageResponse;
import com.example.messagerie.security.JwtHandshakeInterceptor;
import com.example.messagerie.service.MessageService;
import com.example.messagerie.service.NotificationService;
import com.example.messagerie.service.RateLimitService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Canal temps reel ws://.../ws/messages.
 * Le jeton est deja verifie par JwtHandshakeInterceptor, on connait donc toujours l'utilisateur.
 * Types geres : "message" (enregistre puis transmis) et "typing" (transmis sans enregistrement).
 * Le serveur envoie aussi "presence" et "erreur".
 * Les messages sont limites par RateLimitService (anti-spam), et un destinataire hors ligne
 * est prevenu par NotificationService (webhook simule).
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final SessionRegistry sessionRegistry;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final RateLimitService rateLimitService;
    private final NotificationService notificationService;

    public ChatWebSocketHandler(SessionRegistry sessionRegistry,
                                MessageService messageService,
                                ObjectMapper objectMapper,
                                RateLimitService rateLimitService,
                                NotificationService notificationService) {
        this.sessionRegistry = sessionRegistry;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.rateLimitService = rateLimitService;
        this.notificationService = notificationService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long utilisateurId = utilisateurDe(session);
        boolean vientDArriver = sessionRegistry.ajouter(utilisateurId, session);
        log.info("[WS] Utilisateur {} connecte (session {})", utilisateurId, session.getId());

        if (vientDArriver) {
            diffuserPresence(utilisateurId, true);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage texte) {
        Long expediteurId = utilisateurDe(session);
        try {
            JsonNode json = objectMapper.readTree(texte.getPayload());
            String type = json.path("type").asText("");

            switch (type) {
                case "message" -> traiterMessage(expediteurId, json);
                case "typing"  -> traiterSaisie(expediteurId, json);
                default        -> envoyerErreur(expediteurId, session, "Type inconnu : '" + type + "'");
            }
        } catch (IllegalArgumentException e) {
            envoyerErreur(expediteurId, session, e.getMessage());
        } catch (Exception e) {
            log.warn("[WS] Message refuse de l'utilisateur {} : {}", expediteurId, e.getMessage());
            envoyerErreur(expediteurId, session, e.getMessage() != null ? e.getMessage() : "Message invalide");
        }
    }

    // Enregistre le message en base, puis le transmet au destinataire s'il est connecte
    private void traiterMessage(Long expediteurId, JsonNode json) {
        Long destinataireId = json.hasNonNull("destinataireId") ? json.get("destinataireId").asLong() : null;
        String contenu = json.path("contenu").asText(null);

        // Anti-spam : au-dela de la limite, le message est refuse et n'est pas enregistre
        if (!rateLimitService.autoriser(expediteurId)) {
            log.warn("[WS] Limite de debit atteinte pour l'utilisateur {}", expediteurId);
            throw new IllegalArgumentException("Trop de messages envoyes, patientez quelques secondes");
        }

        // L'expediteur vient du jeton, jamais du JSON envoye par le client
        MessageResponse enregistre = messageService.enregistrer(expediteurId, destinataireId, contenu);

        Map<String, Object> sortie = new LinkedHashMap<>();
        sortie.put("type", "message");
        sortie.put("id", enregistre.id());
        sortie.put("expediteurId", enregistre.expediteurId());
        sortie.put("destinataireId", enregistre.destinataireId());
        sortie.put("contenu", enregistre.contenu());
        sortie.put("dateEnvoi", enregistre.dateEnvoi());

        if (sessionRegistry.estConnecte(destinataireId)) {
            envoyerA(sessionRegistry.sessionsDe(destinataireId), sortie);
        } else {
            // Rien n'est perdu : le message est en base et sera lu via l'historique REST
            log.info("[WS] Utilisateur {} hors ligne : message {} conserve en base", destinataireId, enregistre.id());
            notificationService.notifierHorsLigne(destinataireId, expediteurId, enregistre.id());
        }

        // L'expediteur recoit aussi le message, avec l'id et la date du serveur
        envoyerA(sessionRegistry.sessionsDe(expediteurId), sortie);
    }

    // Previent le destinataire que l'expediteur est en train d'ecrire (rien en base)
    private void traiterSaisie(Long expediteurId, JsonNode json) {
        if (!json.hasNonNull("destinataireId")) {
            return;
        }
        Long destinataireId = json.get("destinataireId").asLong();

        Map<String, Object> sortie = new LinkedHashMap<>();
        sortie.put("type", "typing");
        sortie.put("expediteurId", expediteurId);
        sortie.put("destinataireId", destinataireId);
        envoyerA(sessionRegistry.sessionsDe(destinataireId), sortie);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus statut) {
        Long utilisateurId = utilisateurDe(session);
        boolean vientDePartir = sessionRegistry.retirer(utilisateurId, session);
        log.info("[WS] Utilisateur {} deconnecte (session {}, code {})",
                utilisateurId, session.getId(), statut.getCode());

        if (vientDePartir) {
            diffuserPresence(utilisateurId, false);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable erreur) throws IOException {
        log.warn("[WS] Erreur reseau sur la session {} : {}", session.getId(), erreur.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    // Id range dans la session par JwtHandshakeInterceptor
    private Long utilisateurDe(WebSocketSession session) {
        return (Long) session.getAttributes().get(JwtHandshakeInterceptor.ATTRIBUT_UTILISATEUR_ID);
    }

    private void diffuserPresence(Long utilisateurId, boolean connecte) {
        Map<String, Object> sortie = new LinkedHashMap<>();
        sortie.put("type", "presence");
        sortie.put("utilisateurId", utilisateurId);
        sortie.put("connecte", connecte);
        envoyerA(sessionRegistry.toutesLesSessions(), sortie);
    }

    // L'erreur ne part que vers la connexion qui a envoye le message fautif
    private void envoyerErreur(Long utilisateurId, WebSocketSession session, String message) {
        Map<String, Object> sortie = new LinkedHashMap<>();
        sortie.put("type", "erreur");
        sortie.put("message", message);
        Collection<WebSocketSession> cible = sessionRegistry.sessionsDe(utilisateurId).stream()
                .filter(s -> s.getId().equals(session.getId()))
                .toList();
        envoyerA(cible, sortie);
    }

    private void envoyerA(Collection<WebSocketSession> sessions, Map<String, Object> contenu) {
        String json;
        try {
            json = objectMapper.writeValueAsString(contenu);
        } catch (IOException e) {
            log.error("[WS] Impossible de convertir en JSON : {}", e.getMessage());
            return;
        }
        TextMessage message = new TextMessage(json);
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(message);
                }
            } catch (IOException | IllegalStateException e) {
                log.warn("[WS] Envoi impossible vers la session {} : {}", s.getId(), e.getMessage());
            }
        }
    }
}