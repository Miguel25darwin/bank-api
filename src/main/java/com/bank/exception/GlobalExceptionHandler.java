package com.bank.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions HTTP.
 *
 * <p>
 * Centralise la gestion des erreurs pour toute l'application (pattern AOP).
 * Garantit que chaque réponse d'erreur respecte un format JSON cohérent.
 * </p>
 *
 * <pre>
 * {
 *   "timestamp": "2026-04-19T13:30:00",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "code": "INSUFFICIENT_FUNDS",
 *   "message": "Solde insuffisant. Disponible : 50.00 | Demandé : 200.00"
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================================================
    // ERREURS DE VALIDATION (@Valid / @Validated)
    // ============================================================

    /**
     * Gère les erreurs de validation des DTOs (annotation {@code @Valid}).
     * Retourne une map structurée {@code field → message} au lieu d'une chaîne
     * brute.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(field, message);
        });

        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Données de la requête invalides.", fieldErrors);
    }

    // ============================================================
    // EXCEPTIONS MÉTIER TYPÉES
    // ============================================================

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotFound(AccountNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailConflict(EmailAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFunds(InsufficientFundsException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotActive(AccountNotActiveException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getErrorCode(), ex.getMessage(), null);
    }

    // ============================================================
    // CATCH-ALL — Erreurs inattendues
    // ============================================================

    /**
     * Attrape toute exception non prévue et retourne 500.
     * Évite d'exposer des détails internes au client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Une erreur interne est survenue. Veuillez réessayer plus tard.",
                null);
    }

    // ============================================================
    // MÉTHODE UTILITAIRE — Construction de la réponse d'erreur
    // ============================================================

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String code, String message, Object details) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("code", code);
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}