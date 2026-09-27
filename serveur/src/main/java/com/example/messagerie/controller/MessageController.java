package com.example.messagerie.controller;

import com.example.messagerie.dto.MessageResponse;
import com.example.messagerie.service.MessageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * GET /api/messages/{utilisateurId} : historique entre l'utilisateur connecte et cet utilisateur.
     * L'utilisateur connecte ("moi") est lu dans le jeton JWT par JwtAuthFilter :
     * il ne peut donc pas lire les conversations des autres.
     */
    @GetMapping("/{utilisateurId}")
    public List<MessageResponse> historique(@PathVariable Long utilisateurId,
                                            @AuthenticationPrincipal Long moi) {
        return messageService.historique(moi, utilisateurId);
    }
}