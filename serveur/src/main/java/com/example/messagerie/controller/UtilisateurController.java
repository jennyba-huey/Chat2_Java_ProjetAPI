package com.example.messagerie.controller;

import com.example.messagerie.dto.UtilisateurResponse;
import com.example.messagerie.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Utilisateurs", description = "Liste des utilisateurs et statut de connexion")
@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /** GET /api/utilisateurs : liste des utilisateurs avec leur statut connecte / hors ligne. */
    @Operation(
            summary = "Lister les utilisateurs",
            description = "Renvoie tous les utilisateurs avec leur statut connecté ou hors ligne. "
                    + "Codes : 200 succès, 401 jeton absent ou invalide.")
    @GetMapping
    public List<UtilisateurResponse> lister() {
        return utilisateurService.listerAvecStatut();
    }
}