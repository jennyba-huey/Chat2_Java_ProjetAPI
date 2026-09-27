package com.example.messagerie.service;

import com.example.messagerie.dto.MessageResponse;
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

    /** Historique complet entre l'utilisateur courant et un autre utilisateur. */
    @Transactional(readOnly = true)
    public List<MessageResponse> historique(Long moiId, Long autreId) {
        verifierExiste(moiId);
        verifierExiste(autreId);
        return messageRepository.findConversation(moiId, autreId).stream()
                .map(MessageResponse::depuis)
                .toList();
    }

    private void verifierExiste(Long id) {
        if (!utilisateurRepository.existsById(id)) {
            throw new RessourceIntrouvableException("Utilisateur " + id + " introuvable");
        }
    }
}
