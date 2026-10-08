package com.example.messagerie.controller;

import com.example.messagerie.dto.MessageResponse;
import com.example.messagerie.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Messages", description = "Historique des conversations")
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
    @Operation(
            summary = "Historique d'une conversation",
            description = "Renvoie les messages échangés entre l'utilisateur connecté (lu dans le jeton) "
                    + "et l'utilisateur indiqué, du plus ancien au plus récent. "
                    + "Codes : 200 succès, 401 jeton absent ou invalide, 404 utilisateur introuvable.")
    @GetMapping("/{utilisateurId}")
    public List<MessageResponse> historique(
            @Parameter(description = "Id de l'autre utilisateur de la conversation")
            @PathVariable Long utilisateurId,
            @Parameter(hidden = true) // fourni par le jeton, pas par le client
            @AuthenticationPrincipal Long moi) {
        return messageService.historique(moi, utilisateurId);
    }
}