package com.example.messagerie.controller;

import com.example.messagerie.dto.LoginRequest;
import com.example.messagerie.dto.LoginResponse;
import com.example.messagerie.entity.Utilisateur;
import com.example.messagerie.security.JwtService;
import com.example.messagerie.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentification", description = "Connexion et obtention du jeton JWT")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurService utilisateurService;
    private final JwtService jwtService;

    public AuthController(UtilisateurService utilisateurService, JwtService jwtService) {
        this.utilisateurService = utilisateurService;
        this.jwtService = jwtService;
    }

    /** POST /api/auth/login : verifie les identifiants et renvoie un jeton JWT signe. */
    @Operation(
            summary = "Se connecter",
            description = "Vérifie le nom d'utilisateur et le mot de passe, puis renvoie {token, id, username}. "
                    + "Le jeton (valable 24 h) s'envoie ensuite dans l'en-tête Authorization: Bearer <jeton>. "
                    + "Codes : 200 connexion réussie, 400 données invalides, 401 identifiants incorrects.")
    @SecurityRequirements // endpoint public : pas de jeton demande
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest requete) {
        Utilisateur u = utilisateurService.authentifier(requete);
        String jeton = jwtService.genererToken(u.getId(), u.getUsername());
        return new LoginResponse(jeton, u.getId(), u.getUsername());
    }
}