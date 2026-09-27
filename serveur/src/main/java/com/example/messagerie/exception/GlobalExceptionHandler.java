package com.example.messagerie.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

/**
 * Traduit les exceptions Java en reponses HTTP JSON propres,
 * avec le bon code de statut.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ErreurResponse> introuvable(RessourceIntrouvableException e) {
        return construire(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflitException.class)
    public ResponseEntity<ErreurResponse> conflit(ConflitException e) {
        return construire(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ResponseEntity<ErreurResponse> identifiants(IdentifiantsInvalidesException e) {
        return construire(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    /** Erreurs de validation (@NotBlank, @Size...) sur le corps JSON. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurResponse> validation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " : " + f.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return construire(HttpStatus.BAD_REQUEST, details);
    }

    /** Parametre manquant ou mal type dans l'URL (ex : ?moi=abc). */
    @ExceptionHandler({MissingServletRequestParameterException.class,
                       MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErreurResponse> requeteInvalide(Exception e) {
        return construire(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ResponseEntity<ErreurResponse> construire(HttpStatus status, String message) {
        ErreurResponse corps = new ErreurResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return ResponseEntity.status(status).body(corps);
    }
}
