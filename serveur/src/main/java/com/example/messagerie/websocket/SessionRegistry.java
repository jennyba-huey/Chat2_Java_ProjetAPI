package com.example.messagerie.websocket;

import com.example.messagerie.service.PresenceService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carnet d'adresses des connexions WebSocket ouvertes.
 * Pour chaque utilisateur, on garde la liste de ses connexions
 * (il peut etre connecte sur son telephone ET sur son PC en meme temps).
 * Tient aussi a jour PresenceService, utilise par GET /api/utilisateurs.
 */
@Component
public class SessionRegistry {

    /** Temps max (ms) et taille max (octets) d'un envoi avant de considerer la connexion bloquee. */
    private static final int DELAI_ENVOI_MS = 10_000;
    private static final int TAILLE_TAMPON = 512 * 1024;

    /** utilisateurId -> (id de connexion -> connexion) */
    private final Map<Long, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();

    private final PresenceService presenceService;

    public SessionRegistry(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    /**
     * Enregistre une nouvelle connexion.
     * @return true si c'est la premiere connexion de cet utilisateur (il vient de passer "en ligne")
     */
    public boolean ajouter(Long utilisateurId, WebSocketSession session) {
        // Le "decorateur" rend l'envoi sur une connexion sur, meme si deux messages partent en meme temps
        WebSocketSession securisee =
                new ConcurrentWebSocketSessionDecorator(session, DELAI_ENVOI_MS, TAILLE_TAMPON);

        Map<String, WebSocketSession> sessionsUtilisateur =
                sessions.computeIfAbsent(utilisateurId, id -> new ConcurrentHashMap<>());
        boolean premiereConnexion = sessionsUtilisateur.isEmpty();
        sessionsUtilisateur.put(session.getId(), securisee);

        presenceService.marquerConnecte(utilisateurId);
        return premiereConnexion;
    }

    /**
     * Retire une connexion fermee.
     * @return true si c'etait sa derniere connexion (il vient de passer "hors ligne")
     */
    public boolean retirer(Long utilisateurId, WebSocketSession session) {
        Map<String, WebSocketSession> sessionsUtilisateur = sessions.get(utilisateurId);
        if (sessionsUtilisateur == null) {
            return false;
        }
        sessionsUtilisateur.remove(session.getId());

        if (sessionsUtilisateur.isEmpty()) {
            sessions.remove(utilisateurId);
            presenceService.marquerDeconnecte(utilisateurId);
            return true;
        }
        return false;
    }

    /** Toutes les connexions ouvertes d'un utilisateur (liste vide s'il est hors ligne). */
    public Collection<WebSocketSession> sessionsDe(Long utilisateurId) {
        Map<String, WebSocketSession> sessionsUtilisateur = sessions.get(utilisateurId);
        return sessionsUtilisateur == null ? List.of() : sessionsUtilisateur.values();
    }

    /** Toutes les connexions ouvertes, tous utilisateurs confondus (pour diffuser le statut "presence"). */
    public List<WebSocketSession> toutesLesSessions() {
        return sessions.values().stream()
                .flatMap(m -> m.values().stream())
                .toList();
    }

    public boolean estConnecte(Long utilisateurId) {
        return sessions.containsKey(utilisateurId);
    }
}