package com.example.messagerie.exception;

/** Transformee en 401 Unauthorized (mauvais login ou mot de passe). */
public class IdentifiantsInvalidesException extends RuntimeException {
    public IdentifiantsInvalidesException(String message) { super(message); }
}
