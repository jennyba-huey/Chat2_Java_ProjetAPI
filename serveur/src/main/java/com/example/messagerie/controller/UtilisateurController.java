package com.example.messagerie.controller;

import com.example.messagerie.dto.UtilisateurResponse;
import com.example.messagerie.service.UtilisateurService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /** GET /api/utilisateurs : liste des utilisateurs avec leur statut connecte / hors ligne. */
    @GetMapping
    public List<UtilisateurResponse> lister() {
        return utilisateurService.listerAvecStatut();
    }
}
