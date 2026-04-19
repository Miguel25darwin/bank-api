package com.bank.exception;

/**
 * Levée quand on tente de créer un compte avec un email déjà enregistré.
 * Mappée sur {@code 409 Conflict} par le {@link GlobalExceptionHandler}.
 */
public class EmailAlreadyExistsException extends BankException {

    public EmailAlreadyExistsException(String email) {
        super("EMAIL_ALREADY_EXISTS",
                "Un compte avec l'email '" + email + "' existe déjà.");
    }
}
