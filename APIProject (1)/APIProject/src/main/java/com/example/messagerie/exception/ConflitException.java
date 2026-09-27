package com.example.messagerie.exception;

/** Transformee en 409 Conflict (ex : nom d'utilisateur deja pris). */
public class ConflitException extends RuntimeException {
    public ConflitException(String message) { super(message); }
}
