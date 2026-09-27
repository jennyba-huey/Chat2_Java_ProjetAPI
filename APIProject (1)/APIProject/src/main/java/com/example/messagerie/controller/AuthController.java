package com.example.messagerie.controller;

import com.example.messagerie.dto.LoginRequest;
import com.example.messagerie.dto.LoginResponse;
import com.example.messagerie.entity.Utilisateur;
import com.example.messagerie.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurService utilisateurService;

    public AuthController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /** POST /api/auth/login : verifie les identifiants. Le JWT sera ajoute a l'etape 3. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest requete) {
        Utilisateur u = utilisateurService.authentifier(requete);
        return new LoginResponse(null, u.getId(), u.getUsername()); // ETAPE 3 : token JWT ici
    }
}
