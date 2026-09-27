package com.example.messagerie.exception;

import java.time.LocalDateTime;

/** Format JSON unique pour toutes les erreurs de l'API. */
public record ErreurResponse(int status, String erreur, String message, LocalDateTime date) {}
