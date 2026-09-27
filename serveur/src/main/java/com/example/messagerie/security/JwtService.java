package com.example.messagerie.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Fabrique et verifie les jetons JWT.
 * Utilise par le login (AuthController), par le filtre des requetes REST (JwtAuthFilter)
 * et par la verification a l'ouverture du WebSocket (JwtHandshakeInterceptor).
 */
@Service
public class JwtService {

    private final SecretKey cle;
    private final long dureeValiditeMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long dureeValiditeMs) {
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.dureeValiditeMs = dureeValiditeMs;
    }

    /** Cree un jeton signe contenant l'id (sujet) et le nom de l'utilisateur. */
    public String genererToken(Long utilisateurId, String username) {
        Date maintenant = new Date();
        return Jwts.builder()
                .subject(String.valueOf(utilisateurId))
                .claim("username", username)
                .issuedAt(maintenant)
                .expiration(new Date(maintenant.getTime() + dureeValiditeMs))
                .signWith(cle)
                .compact();
    }

    /**
     * Verifie le jeton (signature + date d'expiration).
     * Renvoie l'id de l'utilisateur si le jeton est valide, null sinon.
     */
    public Long extraireUtilisateurId(String token) {
        try {
            Claims contenu = Jwts.parser()
                    .verifyWith(cle)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(contenu.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}