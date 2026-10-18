package com.example.messagerie.dto;

/**
 * Reponse du login. A l'etape 2, token vaut null :
 */
public record LoginResponse(String token, Long id, String username) {}
