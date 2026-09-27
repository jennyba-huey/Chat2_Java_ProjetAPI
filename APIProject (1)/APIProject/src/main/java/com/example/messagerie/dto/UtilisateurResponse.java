package com.example.messagerie.dto;

import com.example.messagerie.entity.Utilisateur;

/** Ce qu'on renvoie d'un utilisateur : jamais son mot de passe. */
public record UtilisateurResponse(Long id, String username, boolean connecte) {

    public static UtilisateurResponse depuis(Utilisateur u, boolean connecte) {
        return new UtilisateurResponse(u.getId(), u.getUsername(), connecte);
    }
}
