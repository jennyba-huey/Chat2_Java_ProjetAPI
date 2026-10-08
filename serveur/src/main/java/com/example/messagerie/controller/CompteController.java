package com.example.messagerie.controller;

import com.example.messagerie.dto.InscriptionRequest;
import com.example.messagerie.dto.UtilisateurResponse;
import com.example.messagerie.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comptes", description = "Création de compte")
@RestController
@RequestMapping("/api/comptes")
public class CompteController {

    private final UtilisateurService utilisateurService;

    public CompteController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /** POST /api/comptes : cree un compte (201 Created). */
    @Operation(
            summary = "Créer un compte",
            description = "Crée un utilisateur ; le mot de passe est haché avec BCrypt avant d'être stocké. "
                    + "Codes : 201 compte créé, 400 données invalides, 409 nom déjà pris.")
    @SecurityRequirements // endpoint public : pas de jeton demande
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurResponse creerCompte(@Valid @RequestBody InscriptionRequest requete) {
        return utilisateurService.inscrire(requete);
    }
}