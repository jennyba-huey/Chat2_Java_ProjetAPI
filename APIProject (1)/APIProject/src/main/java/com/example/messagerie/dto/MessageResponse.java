package com.example.messagerie.dto;

import com.example.messagerie.entity.Message;

import java.time.LocalDateTime;

/** Format JSON d'un message, aligne sur celui impose par la consigne. */
public record MessageResponse(
        Long id,
        Long expediteurId,
        Long destinataireId,
        String contenu,
        LocalDateTime dateEnvoi
) {
    public static MessageResponse depuis(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getExpediteur().getId(),
                m.getDestinataire().getId(),
                m.getContenu(),
                m.getDateEnvoi()
        );
    }
}
