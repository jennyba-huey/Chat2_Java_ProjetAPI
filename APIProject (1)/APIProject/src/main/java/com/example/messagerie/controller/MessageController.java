package com.example.messagerie.controller;

import com.example.messagerie.dto.MessageResponse;
import com.example.messagerie.service.MessageService;
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
     * GET /api/messages/{utilisateurId} : historique avec cet utilisateur.
     * PROVISOIRE (etape 2) : "moi" est passe en parametre ?moi=1.
     * A l'etape 3, il sera lu dans le JWT et ce parametre disparaitra.
     */
    @GetMapping("/{utilisateurId}")
    public List<MessageResponse> historique(@PathVariable Long utilisateurId,
                                            @RequestParam Long moi) {
        return messageService.historique(moi, utilisateurId);
    }
}
