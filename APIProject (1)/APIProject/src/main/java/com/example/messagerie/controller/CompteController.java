package com.example.messagerie.controller;

import com.example.messagerie.dto.InscriptionRequest;
import com.example.messagerie.dto.UtilisateurResponse;
import com.example.messagerie.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comptes")
public class CompteController {

    private final UtilisateurService utilisateurService;

    public CompteController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /** POST /api/comptes : cree un compte (201 Created). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurResponse creerCompte(@Valid @RequestBody InscriptionRequest requete) {
        return utilisateurService.inscrire(requete);
    }
}
