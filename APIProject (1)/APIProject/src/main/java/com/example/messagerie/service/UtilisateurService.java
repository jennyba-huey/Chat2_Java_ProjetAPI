package com.example.messagerie.service;

import com.example.messagerie.dto.InscriptionRequest;
import com.example.messagerie.dto.LoginRequest;
import com.example.messagerie.dto.UtilisateurResponse;
import com.example.messagerie.entity.Utilisateur;
import com.example.messagerie.exception.ConflitException;
import com.example.messagerie.exception.IdentifiantsInvalidesException;
import com.example.messagerie.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final PresenceService presenceService;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                              PasswordEncoder passwordEncoder,
                              PresenceService presenceService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.presenceService = presenceService;
    }

    @Transactional
    public UtilisateurResponse inscrire(InscriptionRequest requete) {
        String username = requete.username().trim();
        if (utilisateurRepository.existsByUsername(username)) {
            throw new ConflitException("Le nom d'utilisateur '" + username + "' est malheureusement deja pris \uD83D\uDE14");
        }
        String hache = passwordEncoder.encode(requete.motDePasse());
        Utilisateur cree = utilisateurRepository.save(new Utilisateur(username, hache));
        return UtilisateurResponse.depuis(cree, false);
    }

    /** Verifie login + mot de passe. Meme message d'erreur dans les deux cas (on ne revele pas si le compte existe). */
    @Transactional(readOnly = true)
    public Utilisateur authentifier(LoginRequest requete) {
        Utilisateur u = utilisateurRepository.findByUsername(requete.username().trim())
                .orElseThrow(() -> new IdentifiantsInvalidesException("Dsl \uD83E\uDD7A ... Identifiant invalide"));
        if (!passwordEncoder.matches(requete.motDePasse(), u.getMotDePasse())) {
            throw new IdentifiantsInvalidesException("Mot de passe invalide \uD83E\uDD7A ");
        }
        return u;
    }

    @Transactional(readOnly = true)
    public List<UtilisateurResponse> listerAvecStatut() {
        return utilisateurRepository.findAllByOrderByUsernameAsc().stream()
                .map(u -> UtilisateurResponse.depuis(u, presenceService.estConnecte(u.getId())))
                .toList();
    }
}
