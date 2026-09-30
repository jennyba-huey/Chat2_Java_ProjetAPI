package com.example.messagerie.service;

import com.example.messagerie.dto.MessageResponse;
import com.example.messagerie.entity.Message;
import com.example.messagerie.entity.Utilisateur;
import com.example.messagerie.exception.RessourceIntrouvableException;
import com.example.messagerie.repository.MessageRepository;
import com.example.messagerie.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UtilisateurRepository utilisateurRepository;

    public MessageService(MessageRepository messageRepository,
                          UtilisateurRepository utilisateurRepository) {
        this.messageRepository = messageRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    /** Historique complet entre l'utilisateur courant et un autre utilisateur (REST). */
    @Transactional(readOnly = true)
    public List<MessageResponse> historique(Long moiId, Long autreId) {
        verifierExiste(moiId);
        verifierExiste(autreId);
        return messageRepository.findConversation(moiId, autreId).stream()
                .map(MessageResponse::depuis)
                .toList();
    }

    /**
     * Enregistre un message recu par WebSocket (etape 4).
     * L'expediteur vient du jeton JWT (jamais du client),
     * la date est posee par le serveur grace a @PrePersist dans l'entite Message.
     */
    @Transactional
    public MessageResponse enregistrer(Long expediteurId, Long destinataireId, String contenu) {
        if (contenu == null || contenu.isBlank()) {
            throw new IllegalArgumentException("Le message est vide");
        }
        if (contenu.length() > 2000) {
            throw new IllegalArgumentException("Le message depasse 2000 caracteres");
        }
        if (destinataireId == null) {
            throw new IllegalArgumentException("Destinataire manquant");
        }
        if (destinataireId.equals(expediteurId)) {
            throw new IllegalArgumentException("Impossible de s'envoyer un message a soi-meme");
        }

        Utilisateur expediteur = utilisateurRepository.findById(expediteurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Expediteur introuvable"));
        Utilisateur destinataire = utilisateurRepository.findById(destinataireId)
                .orElseThrow(() -> new RessourceIntrouvableException("Destinataire introuvable"));

        Message sauvegarde = messageRepository.save(new Message(expediteur, destinataire, contenu.trim()));
        return MessageResponse.depuis(sauvegarde);
    }

    private void verifierExiste(Long id) {
        if (!utilisateurRepository.existsById(id)) {
            throw new RessourceIntrouvableException("Utilisateur " + id + " introuvable");
        }
    }
}