package com.example.messagerie.controller;

import com.example.messagerie.dto.LoginRequest;
import com.example.messagerie.dto.LoginResponse;
import com.example.messagerie.entity.Utilisateur;
import com.example.messagerie.security.JwtService;
import com.example.messagerie.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest requete) {
        Utilisateur u = utilisateurService.authentifier(requete);
        String jeton = jwtService.genererToken(u.getId(), u.getUsername());
        return new LoginResponse(jeton, u.getId(), u.getUsername());
    }
}