package com.bank.exception;

import java.util.UUID;

/**
 * Levée quand un compte bancaire est introuvable par son identifiant.
 * Mappée sur {@code 404 Not Found} par le {@link GlobalExceptionHandler}.
 */
public class AccountNotFoundException extends BankException {

    public AccountNotFoundException(UUID id) {
        super("ACCOUNT_NOT_FOUND",
                "Aucun compte trouvé avec l'identifiant : " + id);
    }
}
