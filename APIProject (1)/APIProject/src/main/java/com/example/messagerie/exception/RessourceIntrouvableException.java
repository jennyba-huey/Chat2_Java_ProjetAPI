package com.example.messagerie.exception;

/** Transformee en 404 Not Found par GlobalExceptionHandler. */
public class RessourceIntrouvableException extends RuntimeException {
    public RessourceIntrouvableException(String message) { super(message); }
}
