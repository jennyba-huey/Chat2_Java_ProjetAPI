package com.example.messagerie.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lit l'en-tete "Authorization: Bearer <jeton>" a chaque requete REST.
 * Si le jeton est valide, l'utilisateur est considere comme authentifie :
 * son id devient le "principal", que les controleurs recuperent avec @AuthenticationPrincipal.
 * Si le jeton est absent ou invalide, on ne fait rien : SecurityConfig renverra 401
 * pour les endpoints proteges.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete,
                                    HttpServletResponse reponse,
                                    FilterChain chaine) throws ServletException, IOException {

        String entete = requete.getHeader("Authorization");

        if (entete != null && entete.startsWith(PREFIXE)) {
            String jeton = entete.substring(PREFIXE.length());
            Long utilisateurId = jwtService.extraireUtilisateurId(jeton);

            if (utilisateurId != null) {
                var authentification = new UsernamePasswordAuthenticationToken(
                        utilisateurId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentification);
            }
        }

        chaine.doFilter(requete, reponse);
    }
}