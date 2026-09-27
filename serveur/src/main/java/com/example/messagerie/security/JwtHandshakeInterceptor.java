package com.example.messagerie.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Verifie le jeton JWT a l'ouverture du WebSocket (ws://.../ws/messages?token=<jeton>).
 * - Jeton absent ou invalide : la connexion est refusee (401).
 * - Jeton valide : l'id de l'utilisateur est range dans les attributs de la session WebSocket,
 *   sous la cle ATTRIBUT_UTILISATEUR_ID.
 *
 * Utilisation cote WebSocket (Djeneba) :
 *   - dans WebSocketConfig : .addInterceptors(jwtHandshakeInterceptor)
 *   - dans le handler : Long id = (Long) session.getAttributes()
 *                                   .get(JwtHandshakeInterceptor.ATTRIBUT_UTILISATEUR_ID);
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    /** Nom de l'attribut qui contient l'id de l'utilisateur connecte. */
    public static final String ATTRIBUT_UTILISATEUR_ID = "utilisateurId";

    private final JwtService jwtService;

    public JwtHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest requete,
                                   ServerHttpResponse reponse,
                                   WebSocketHandler handler,
                                   Map<String, Object> attributs) {

        String jeton = UriComponentsBuilder.fromUri(requete.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");

        Long utilisateurId = (jeton == null) ? null : jwtService.extraireUtilisateurId(jeton);

        if (utilisateurId == null) {
            reponse.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributs.put(ATTRIBUT_UTILISATEUR_ID, utilisateurId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest requete,
                               ServerHttpResponse reponse,
                               WebSocketHandler handler,
                               Exception exception) {
        // Rien a faire apres l'ouverture de la connexion.
    }
}