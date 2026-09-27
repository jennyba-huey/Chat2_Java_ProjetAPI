package com.example.messagerie.dto;

/**
 * Reponse du login. A l'etape 2, token vaut null :
 * il sera rempli par le JWT a l'etape 3.
 */
public record LoginResponse(String token, Long id, String username) {}
